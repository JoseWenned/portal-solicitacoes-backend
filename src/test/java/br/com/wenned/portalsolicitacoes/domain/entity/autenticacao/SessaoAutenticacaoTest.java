package br.com.wenned.portalsolicitacoes.domain.entity.autenticacao;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessaoAutenticacaoTest {

    private static final Instant NOW =
        Instant.parse("2026-10-03T06:00:00Z");

    private static final Duration VALIDADE = Duration.ofHours(8);
    private static final String HASH = "a".repeat(64);
    private static final String NOVO_HASH = "b".repeat(64);

    @Test
    void deveCriarSessaoAtivaComValidadeAbsoluta() {
        UUID usuarioId = UUID.randomUUID();

        var sessao = SessaoAutenticacao.criar(
            usuarioId, HASH, NOW, VALIDADE
        );

        assertThat(sessao.getId()).isNotNull();
        assertThat(sessao.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(sessao.getCreatedAt()).isEqualTo(NOW);
        assertThat(sessao.getExpiresAt()).isEqualTo(NOW.plus(VALIDADE));
        assertThat(sessao.getVersion()).isZero();
        assertThat(sessao.estaAtiva(NOW)).isTrue();
        assertThat(sessao.toString()).doesNotContain(HASH);
    }

    @Test
    void deveConsiderarSessaoInativaNoInstanteDaExpiracao() {
        var sessao = novaSessao();

        assertThat(sessao.estaAtiva(
            sessao.getExpiresAt().minusNanos(1)
        )).isTrue();

        assertThat(sessao.estaAtiva(sessao.getExpiresAt())).isFalse();
    }

    @Test
    void deveRotacionarTokenSemEstenderValidade() {
        var original = novaSessao();

        var atualizada = original.rotacionarRefreshToken(
            NOVO_HASH,
            NOW.plus(Duration.ofMinutes(15))
        );

        assertThat(atualizada.getId()).isEqualTo(original.getId());
        assertThat(atualizada.getRefreshTokenHash()).isEqualTo(NOVO_HASH);
        assertThat(atualizada.getExpiresAt())
            .isEqualTo(original.getExpiresAt());

        assertThat(original.getRefreshTokenHash()).isEqualTo(HASH);
    }

    @Test
    void deveRevogarSessaoEImpedirRenovacao() {
        var sessao = novaSessao();
        Instant logoutAt = NOW.plusSeconds(60);

        var revogada = sessao.revogar(logoutAt);

        assertThat(revogada.getRevokedAt()).isEqualTo(logoutAt);
        assertThat(revogada.estaAtiva(logoutAt)).isFalse();

        assertThatThrownBy(() ->
            revogada.rotacionarRefreshToken(
                NOVO_HASH, logoutAt.plusSeconds(1)
            )
        ).isInstanceOf(IllegalStateException.class);

        assertThat(revogada.revogar(logoutAt.plusSeconds(1)))
            .isSameAs(revogada);
    }

    @Test
    void deveImpedirRotacaoDeSessaoExpirada() {
        var sessao = novaSessao();

        assertThatThrownBy(() ->
            sessao.rotacionarRefreshToken(
                NOVO_HASH, sessao.getExpiresAt()
            )
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveRecusarHashInvalido() {
        assertThatThrownBy(() ->
            SessaoAutenticacao.criar(
                UUID.randomUUID(),
                "token-sem-hash",
                NOW,
                VALIDADE
            )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRecusarValidadeNaoPositiva() {
        assertThatThrownBy(() ->
            SessaoAutenticacao.criar(
                UUID.randomUUID(), HASH, NOW, Duration.ZERO
            )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    private SessaoAutenticacao novaSessao() {
        return SessaoAutenticacao.criar(
            UUID.randomUUID(), HASH, NOW, VALIDADE
        );
    }
}