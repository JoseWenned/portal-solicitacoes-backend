package br.com.wenned.portalsolicitacoes.infrastructure.persistence.adapter.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.port.out.solicitacoes.SolicitacaoRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.CategoriaSolicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.Solicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.StatusSolicitacao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class SolicitacaoRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    SolicitacaoRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void devePersistirSolicitacoesERetornarCodigosGeradosPeloBanco() {
        UUID solicitanteId = criarUsuario();
        Instant agora = Instant.parse("2026-10-03T15:00:00Z");

        var primeira = Solicitacao.criar(
            "Acesso ao sistema",
            "Preciso de acesso ao sistema interno.",
            CategoriaSolicitacao.TI,
            solicitanteId,
            agora
        );

        var segunda = Solicitacao.criar(
            "Compra de teclado",
            "Solicito um teclado para minha estação.",
            CategoriaSolicitacao.COMPRAS,
            solicitanteId,
            agora
        );

        var primeiraPersistida = repository.criar(primeira);
        var segundaPersistida = repository.criar(segunda);

        assertThat(primeiraPersistida.getId()).isEqualTo(primeira.getId());
        assertThat(segundaPersistida.getId()).isEqualTo(segunda.getId());

        assertThat(primeiraPersistida.getCodigo()).isNotNull().isPositive();
        assertThat(segundaPersistida.getCodigo()).isNotNull().isPositive();
        assertThat(segundaPersistida.getCodigo())
            .isNotEqualTo(primeiraPersistida.getCodigo());

        verificarRegistro(primeiraPersistida);
        verificarRegistro(segundaPersistida);
    }

    private UUID criarUsuario() {
        UUID id = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO usuarios (id, name, email, password_hash)
            VALUES (?, ?, ?, ?)
            """,
            id,
            "Ana",
            "persistencia-" + id + "@example.com",
            "hash-exclusivo-da-fixture-de-persistencia"
        );

        return id;
    }

    private void verificarRegistro(Solicitacao solicitacao) {
        Integer quantidade = jdbcTemplate.queryForObject("""
            SELECT count(*) FROM solicitacoes WHERE id = ?
            """, Integer.class, solicitacao.getId());

        assertThat(quantidade).isEqualTo(1);

        jdbcTemplate.queryForObject("""
            SELECT id, codigo, titulo, descricao, categoria, status,
                   solicitante_id, created_at, updated_at, version
            FROM solicitacoes
            WHERE id = ?
            """,
            (rs, rowNum) -> {
                assertThat(rs.getObject("id", UUID.class))
                    .isEqualTo(solicitacao.getId());

                assertThat(rs.getLong("codigo"))
                    .isEqualTo(solicitacao.getCodigo().longValue());

                assertThat(rs.getString("titulo"))
                    .isEqualTo(solicitacao.getTitulo());

                assertThat(rs.getString("descricao"))
                    .isEqualTo(solicitacao.getDescricao());

                assertThat(rs.getString("categoria"))
                    .isEqualTo(solicitacao.getCategoria().name());

                assertThat(rs.getString("status"))
                    .isEqualTo(StatusSolicitacao.ABERTO.name());

                assertThat(rs.getObject("solicitante_id", UUID.class))
                    .isEqualTo(solicitacao.getSolicitanteId());

                assertThat(
                    rs.getObject("created_at", OffsetDateTime.class)
                        .toInstant()
                ).isEqualTo(solicitacao.getCreatedAt());

                assertThat(
                    rs.getObject("updated_at", OffsetDateTime.class)
                        .toInstant()
                ).isEqualTo(solicitacao.getUpdatedAt());

                assertThat(rs.getLong("version")).isZero();

                return true;
            },
            solicitacao.getId()
        );
    }
}