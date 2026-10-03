package br.com.wenned.portalsolicitacoes.infrastructure.persistence.adapter;

import org.springframework.test.context.ActiveProfiles;

import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.SessaoAutenticacaoRepository;
import br.com.wenned.portalsolicitacoes.application.port.out.usuarios.UsuarioRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.autenticacao.SessaoAutenticacao;
import br.com.wenned.portalsolicitacoes.domain.entity.usuarios.Usuario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class SessaoAutenticacaoRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static final Instant NOW =
        Instant.parse("2026-10-03T06:00:00Z");

    @Autowired
    SessaoAutenticacaoRepository sessoes;

    @Autowired
    UsuarioRepository usuarios;

    @Test
    void deveCriarEConsultarSessao() {
        var sessao = criarSessao();

        var encontrada = sessoes.findById(sessao.getId()).orElseThrow();

        assertThat(encontrada.getUsuarioId())
            .isEqualTo(sessao.getUsuarioId());
        assertThat(encontrada.getRefreshTokenHash())
            .isEqualTo(sessao.getRefreshTokenHash());
        assertThat(encontrada.getExpiresAt())
            .isEqualTo(sessao.getExpiresAt());
        assertThat(encontrada.getVersion()).isZero();
        assertThat(encontrada.getRevokedAt()).isNull();

        assertThat(sessoes.findByRefreshTokenHash(
            sessao.getRefreshTokenHash()
        )).isPresent();
    }

    @Test
    void deveRotacionarUmaVezSemEstenderExpiracao() {
        var original = criarSessao();
        String novoHash = novoHash();

        boolean alterou = sessoes.rotateRefreshToken(
            original.getId(),
            original.getVersion(),
            original.getRefreshTokenHash(),
            novoHash,
            NOW.plusSeconds(60)
        );

        assertThat(alterou).isTrue();

        var atualizada = sessoes.findById(original.getId()).orElseThrow();

        assertThat(atualizada.getRefreshTokenHash()).isEqualTo(novoHash);
        assertThat(atualizada.getVersion()).isEqualTo(1);
        assertThat(atualizada.getExpiresAt())
            .isEqualTo(original.getExpiresAt());

        assertThat(sessoes.findByRefreshTokenHash(
            original.getRefreshTokenHash()
        )).isEmpty();

        assertThat(sessoes.findByRefreshTokenHash(novoHash)).isPresent();

        boolean reutilizouTokenAntigo = sessoes.rotateRefreshToken(
            original.getId(),
            original.getVersion(),
            original.getRefreshTokenHash(),
            novoHash(),
            NOW.plusSeconds(120)
        );

        assertThat(reutilizouTokenAntigo).isFalse();
    }

    @Test
    void deveRecusarRotacaoComVersaoOuHashIncorretos() {
        var original = criarSessao();

        assertThat(sessoes.rotateRefreshToken(
            original.getId(),
            original.getVersion() + 1,
            original.getRefreshTokenHash(),
            novoHash(),
            NOW.plusSeconds(60)
        )).isFalse();

        assertThat(sessoes.rotateRefreshToken(
            original.getId(),
            original.getVersion(),
            novoHash(),
            novoHash(),
            NOW.plusSeconds(60)
        )).isFalse();

        var preservada = sessoes.findById(original.getId()).orElseThrow();

        assertThat(preservada.getVersion()).isZero();
        assertThat(preservada.getRefreshTokenHash())
            .isEqualTo(original.getRefreshTokenHash());
    }

    @Test
    void deveRecusarRotacaoNoInstanteDaExpiracao() {
        var sessao = criarSessao();

        boolean alterou = sessoes.rotateRefreshToken(
            sessao.getId(),
            sessao.getVersion(),
            sessao.getRefreshTokenHash(),
            novoHash(),
            sessao.getExpiresAt()
        );

        assertThat(alterou).isFalse();

        var preservada = sessoes.findById(sessao.getId()).orElseThrow();
        assertThat(preservada.getVersion()).isZero();
    }

    @Test
    void deveRevogarUmaVezEImpedirRotacao() {
        var sessao = criarSessao();
        Instant logoutAt = NOW.plusSeconds(60);

        sessoes.revoke(sessao.getId(), logoutAt);
        sessoes.revoke(sessao.getId(), logoutAt.plusSeconds(30));

        var revogada = sessoes.findById(sessao.getId()).orElseThrow();

        assertThat(revogada.getRevokedAt()).isEqualTo(logoutAt);
        assertThat(revogada.getVersion()).isEqualTo(1);
        assertThat(revogada.estaAtiva(logoutAt)).isFalse();

        assertThat(sessoes.rotateRefreshToken(
            revogada.getId(),
            revogada.getVersion(),
            revogada.getRefreshTokenHash(),
            novoHash(),
            logoutAt.plusSeconds(60)
        )).isFalse();
    }

    @Test
    void deveRevogarSessaoMesmoAposRotacao() {
        var original = criarSessao();

        assertThat(sessoes.rotateRefreshToken(
            original.getId(),
            original.getVersion(),
            original.getRefreshTokenHash(),
            novoHash(),
            NOW.plusSeconds(60)
        )).isTrue();

        Instant logoutAt = NOW.plusSeconds(120);
        sessoes.revoke(original.getId(), logoutAt);

        var revogada = sessoes.findById(original.getId()).orElseThrow();

        assertThat(revogada.getRevokedAt()).isEqualTo(logoutAt);
        assertThat(revogada.getVersion()).isEqualTo(2);
        assertThat(revogada.estaAtiva(logoutAt)).isFalse();
    }

    private SessaoAutenticacao criarSessao() {
        Usuario usuario = usuarios.save(Usuario.criar(
            "Ana",
            "sessao-" + UUID.randomUUID() + "@example.com",
            "hash-ficticio-exclusivo-do-teste-de-persistencia",
            NOW
        ));

        return sessoes.create(SessaoAutenticacao.criar(
            usuario.getId(),
            novoHash(),
            NOW,
            Duration.ofHours(8)
        ));
    }

    private String novoHash() {
        return UUID.randomUUID().toString().replace("-", "")
            + UUID.randomUUID().toString().replace("-", "");
    }
}