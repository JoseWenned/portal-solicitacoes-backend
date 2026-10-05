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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class SolicitacaoOperacoesRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:16");

    private static final Instant CRIACAO =
        Instant.parse("2026-10-05T07:00:00Z");

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
    void deveRecusarAsTresOperacoesComVersaoAntiga() {
        var original = criarSolicitacao();

        var primeiraEdicao = original.editar(
            "Primeira edição",
            "Descrição atualizada.",
            CategoriaSolicitacao.RH,
            CRIACAO.plusSeconds(1)
        );

        assertThat(repository.editar(primeiraEdicao)).isTrue();

        // Continua utilizando a versão zero lida antes da primeira edição.
        var edicaoAntiga = original.editar(
            "Edição que não deve prevalecer",
            "Descrição antiga.",
            CategoriaSolicitacao.TI,
            CRIACAO.plusSeconds(2)
        );

        var transicaoAntiga = original.alterarStatus(
            StatusSolicitacao.EM_ATENDIMENTO,
            CRIACAO.plusSeconds(2)
        );

        assertThat(repository.editar(edicaoAntiga)).isFalse();

        assertThat(repository.alterarStatus(
            transicaoAntiga,
            StatusSolicitacao.ABERTO
        )).isFalse();

        assertThat(repository.excluir(
            original.getId(),
            original.getSolicitanteId(),
            original.getVersion()
        )).isFalse();

        var atual = consultar(original);

        assertThat(atual.getTitulo()).isEqualTo("Primeira edição");
        assertThat(atual.getCategoria()).isEqualTo(CategoriaSolicitacao.RH);
        assertThat(atual.getStatus()).isEqualTo(StatusSolicitacao.ABERTO);
        assertThat(atual.getVersion()).isEqualTo(1);
        assertThat(atual.getUpdatedAt()).isEqualTo(CRIACAO.plusSeconds(1));
    }

    @Test
    void deveVerificarStatusMesmoQuandoAVersaoCorresponde() {
        var original = criarSolicitacao();

        var emAtendimento = original.alterarStatus(
            StatusSolicitacao.EM_ATENDIMENTO,
            CRIACAO.plusSeconds(1)
        );

        assertThat(repository.alterarStatus(
            emAtendimento,
            StatusSolicitacao.ABERTO
        )).isTrue();

        var atual = consultar(original);

        // Simula uma entrada de persistência com a versão atual,
        // mas com a expectativa incorreta de que o registro está aberto.
        var expectativaAberta = Solicitacao.reconstituir(
            atual.getId(),
            atual.getCodigo(),
            atual.getTitulo(),
            atual.getDescricao(),
            atual.getCategoria(),
            StatusSolicitacao.ABERTO,
            atual.getSolicitanteId(),
            atual.getCreatedAt(),
            atual.getUpdatedAt(),
            atual.getVersion()
        );

        var edicao = expectativaAberta.editar(
            "Não deve ser salvo",
            "Descrição",
            CategoriaSolicitacao.RH,
            CRIACAO.plusSeconds(2)
        );

        assertThat(repository.editar(edicao)).isFalse();

        assertThat(repository.excluir(
            atual.getId(),
            atual.getSolicitanteId(),
            atual.getVersion()
        )).isFalse();

        var transicaoComEstadoIncorreto = expectativaAberta.alterarStatus(
            StatusSolicitacao.EM_ATENDIMENTO,
            CRIACAO.plusSeconds(2)
        );

        assertThat(repository.alterarStatus(
            transicaoComEstadoIncorreto,
            StatusSolicitacao.ABERTO
        )).isFalse();

        var preservada = consultar(original);

        assertThat(preservada.getTitulo()).isEqualTo("Título original");
        assertThat(preservada.getStatus())
            .isEqualTo(StatusSolicitacao.EM_ATENDIMENTO);
        assertThat(preservada.getVersion()).isEqualTo(1);

        // Com estado e versão corretos, a próxima transição funciona.
        var concluida = preservada.alterarStatus(
            StatusSolicitacao.CONCLUIDO,
            CRIACAO.plusSeconds(3)
        );

        assertThat(repository.alterarStatus(
            concluida,
            StatusSolicitacao.EM_ATENDIMENTO
        )).isTrue();

        assertThat(consultar(original).getVersion()).isEqualTo(2);
        assertThat(consultar(original).getStatus())
            .isEqualTo(StatusSolicitacao.CONCLUIDO);
    }

    @Test
    void deveVerificarProprietarioNasTresOperacoesDeEscrita() {
        var original = criarSolicitacao();
        UUID outroUsuarioId = criarUsuario();

        var outroProprietario = Solicitacao.reconstituir(
            original.getId(),
            original.getCodigo(),
            original.getTitulo(),
            original.getDescricao(),
            original.getCategoria(),
            original.getStatus(),
            outroUsuarioId,
            original.getCreatedAt(),
            original.getUpdatedAt(),
            original.getVersion()
        );

        var edicao = outroProprietario.editar(
            "Não deve ser salvo",
            "Descrição",
            CategoriaSolicitacao.RH,
            CRIACAO.plusSeconds(1)
        );

        var transicao = outroProprietario.alterarStatus(
            StatusSolicitacao.EM_ATENDIMENTO,
            CRIACAO.plusSeconds(1)
        );

        assertThat(repository.editar(edicao)).isFalse();

        assertThat(repository.alterarStatus(
            transicao,
            StatusSolicitacao.ABERTO
        )).isFalse();

        assertThat(repository.excluir(
            original.getId(),
            outroUsuarioId,
            original.getVersion()
        )).isFalse();

        var preservada = consultar(original);

        assertThat(preservada.getSolicitanteId())
            .isEqualTo(original.getSolicitanteId());
        assertThat(preservada.getTitulo()).isEqualTo("Título original");
        assertThat(preservada.getStatus()).isEqualTo(StatusSolicitacao.ABERTO);
        assertThat(preservada.getVersion()).isZero();
    }

    private Solicitacao criarSolicitacao() {
        return repository.criar(Solicitacao.criar(
            "Título original",
            "Descrição original",
            CategoriaSolicitacao.TI,
            criarUsuario(),
            CRIACAO
        ));
    }

    private Solicitacao consultar(Solicitacao original) {
        return repository.consultarPorIdEProprietario(
            original.getId(),
            original.getSolicitanteId()
        ).orElseThrow();
    }

    private UUID criarUsuario() {
        UUID id = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO usuarios (id, name, email, password_hash)
            VALUES (?, ?, ?, ?)
            """,
            id,
            "Ana",
            "operacao-persistencia-" + id + "@example.com",
            "hash-exclusivo-da-fixture-de-persistencia"
        );

        return id;
    }
}