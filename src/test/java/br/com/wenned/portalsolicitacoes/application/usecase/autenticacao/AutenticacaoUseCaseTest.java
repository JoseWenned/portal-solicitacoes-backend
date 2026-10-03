package br.com.wenned.portalsolicitacoes.application.usecase.autenticacao;

import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.AccessTokenProvider;
import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.RefreshTokenProvider;
import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.SessaoAutenticacaoRepository;
import br.com.wenned.portalsolicitacoes.application.port.out.security.PasswordHasher;
import br.com.wenned.portalsolicitacoes.application.port.out.usuarios.UsuarioRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.autenticacao.SessaoAutenticacao;
import br.com.wenned.portalsolicitacoes.domain.entity.usuarios.Usuario;
import br.com.wenned.portalsolicitacoes.domain.exception.autenticacao.AutenticacaoInvalidaException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacaoUseCaseTest {

    private static final Instant NOW =
        Instant.parse("2026-10-03T09:00:00Z");

    private static final String HASH_ATUAL = "a".repeat(64);
    private static final String HASH_NOVO = "b".repeat(64);

    @Mock
    UsuarioRepository usuarios;

    @Mock
    SessaoAutenticacaoRepository sessoes;

    @Mock
    PasswordHasher passwordHasher;

    @Mock
    RefreshTokenProvider refreshTokens;

    @Mock
    AccessTokenProvider accessTokens;

    AutenticacaoUseCase useCase;

    Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.criar(
            "Ana",
            "ana@example.com",
            "hash-bcrypt-de-teste",
            NOW.minusSeconds(3600)
        );

        useCase = new AutenticacaoUseCase(
            usuarios,
            sessoes,
            passwordHasher,
            refreshTokens,
            accessTokens,
            Clock.fixed(NOW, ZoneOffset.UTC),
            Duration.ofMinutes(15),
            Duration.ofHours(8),
            "hash-auxiliar-de-teste"
        );
    }

    @Test
    void deveFazerLoginComEmailNormalizadoECriarSessao() {
        when(usuarios.findByEmail("ana@example.com"))
            .thenReturn(Optional.of(usuario));

        when(passwordHasher.matches(
            "Teste12345!", usuario.getPasswordHash()
        )).thenReturn(true);

        when(refreshTokens.generate()).thenReturn("refresh-original");
        when(refreshTokens.hash("refresh-original")).thenReturn(HASH_ATUAL);

        when(accessTokens.generate(
            eq(usuario.getId()),
            any(),
            eq(NOW),
            eq(NOW.plusSeconds(900))
        )).thenReturn("jwt-assinado");

        var resultado = useCase.login(
            " ANA@EXAMPLE.COM ", "Teste12345!"
        );

        ArgumentCaptor<SessaoAutenticacao> captor =
            ArgumentCaptor.forClass(SessaoAutenticacao.class);

        verify(sessoes).create(captor.capture());

        var criada = captor.getValue();

        assertThat(criada.getUsuarioId()).isEqualTo(usuario.getId());
        assertThat(criada.getRefreshTokenHash()).isEqualTo(HASH_ATUAL);
        assertThat(criada.getExpiresAt()).isEqualTo(NOW.plusSeconds(28800));

        assertThat(resultado.accessToken()).isEqualTo("jwt-assinado");
        assertThat(resultado.refreshToken()).isEqualTo("refresh-original");
        assertThat(resultado.expiresIn()).isEqualTo(900);
        assertThat(resultado.usuario().id()).isEqualTo(usuario.getId());

        verify(accessTokens).generate(
            usuario.getId(),
            criada.getId(),
            NOW,
            NOW.plusSeconds(900)
        );
    }

    @Test
    void deveRecusarSenhaIncorretaSemCriarSessao() {
        when(usuarios.findByEmail("ana@example.com"))
            .thenReturn(Optional.of(usuario));

        when(passwordHasher.matches(
            "senha-incorreta", usuario.getPasswordHash()
        )).thenReturn(false);

        assertThatThrownBy(() ->
            useCase.login("ana@example.com", "senha-incorreta")
        ).isInstanceOf(AutenticacaoInvalidaException.class);

        verifyNoInteractions(sessoes, refreshTokens, accessTokens);
    }

    @Test
    void deveCompararHashAuxiliarQuandoUsuarioNaoExiste() {
        when(usuarios.findByEmail("ausente@example.com"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            useCase.login("ausente@example.com", "Teste12345!")
        ).isInstanceOf(AutenticacaoInvalidaException.class);

        verify(passwordHasher).matches(
            "Teste12345!", "hash-auxiliar-de-teste"
        );

        verifyNoInteractions(sessoes, refreshTokens, accessTokens);
    }

    @Test
    void deveRenovarLimitandoJwtPelaExpiracaoDaSessao() {
        var sessao = sessaoComExpiracao(NOW.plusSeconds(120));

        prepararRenovacao(sessao);

        when(accessTokens.generate(
            usuario.getId(),
            sessao.getId(),
            NOW,
            sessao.getExpiresAt()
        )).thenReturn("novo-jwt");

        when(sessoes.rotateRefreshToken(
            sessao.getId(),
            sessao.getVersion(),
            HASH_ATUAL,
            HASH_NOVO,
            NOW
        )).thenReturn(true);

        var resultado = useCase.renovar("refresh-atual");

        assertThat(resultado.expiresIn()).isEqualTo(120);
        assertThat(resultado.sessionExpiresAt())
            .isEqualTo(sessao.getExpiresAt());
        assertThat(resultado.refreshToken()).isEqualTo("novo-refresh");
        assertThat(resultado.accessToken()).isEqualTo("novo-jwt");
    }

    @Test
    void deveRecusarRenovacaoDeSessaoExpirada() {
        var sessao = sessaoComExpiracao(NOW);

        when(refreshTokens.hash("refresh-atual")).thenReturn(HASH_ATUAL);
        when(sessoes.findByRefreshTokenHash(HASH_ATUAL))
            .thenReturn(Optional.of(sessao));

        assertThatThrownBy(() ->
            useCase.renovar("refresh-atual")
        ).isInstanceOf(AutenticacaoInvalidaException.class);

        verify(refreshTokens, never()).generate();
        verifyNoInteractions(usuarios, accessTokens);
        verify(sessoes, never()).rotateRefreshToken(
            any(), anyLong(), anyString(), anyString(), any()
        );
    }

    @Test
    void deveRecusarRenovacaoDeSessaoRevogada() {
        var sessao = sessaoComExpiracao(NOW.plusSeconds(3600))
            .revogar(NOW.minusSeconds(1));

        when(refreshTokens.hash("refresh-atual")).thenReturn(HASH_ATUAL);
        when(sessoes.findByRefreshTokenHash(HASH_ATUAL))
            .thenReturn(Optional.of(sessao));

        assertThatThrownBy(() ->
            useCase.renovar("refresh-atual")
        ).isInstanceOf(AutenticacaoInvalidaException.class);

        verify(refreshTokens, never()).generate();
        verifyNoInteractions(usuarios, accessTokens);
    }

    @Test
    void deveRecusarRenovacaoQuandoAtualizacaoAtomicaFalha() {
        var sessao = sessaoComExpiracao(NOW.plusSeconds(3600));

        prepararRenovacao(sessao);

        when(accessTokens.generate(
            usuario.getId(),
            sessao.getId(),
            NOW,
            NOW.plusSeconds(900)
        )).thenReturn("jwt-nao-entregue");

        // O mock retorna false: outra operação pode ter alterado a sessão.
        assertThatThrownBy(() ->
                useCase.renovar("refresh-atual")
        ).isInstanceOf(AutenticacaoInvalidaException.class);

        verify(sessoes).rotateRefreshToken(
            sessao.getId(),
            sessao.getVersion(),
            HASH_ATUAL,
            HASH_NOVO,
            NOW
        );
    }

    @Test
    void deveRevogarSessaoNoLogout() {
        var sessao = sessaoComExpiracao(NOW.plusSeconds(3600));

        when(refreshTokens.hash("refresh-atual")).thenReturn(HASH_ATUAL);
        when(sessoes.findByRefreshTokenHash(HASH_ATUAL))
            .thenReturn(Optional.of(sessao));

        useCase.logout("refresh-atual");

        verify(sessoes).revoke(sessao.getId(), NOW);
        verifyNoInteractions(usuarios, passwordHasher, accessTokens);
    }

    @Test
    void deveAceitarLogoutSemCookie() {
        useCase.logout(null);
        useCase.logout("");

        verifyNoInteractions(
            sessoes, usuarios, passwordHasher,
            refreshTokens, accessTokens
        );
    }

    @Test
    void deveAceitarLogoutComTokenDesconhecido() {
        when(refreshTokens.hash("desconhecido")).thenReturn(HASH_ATUAL);
        when(sessoes.findByRefreshTokenHash(HASH_ATUAL))
            .thenReturn(Optional.empty());

        useCase.logout("desconhecido");

        verify(sessoes, never()).revoke(any(), any());
    }

    private SessaoAutenticacao sessaoComExpiracao(Instant expiresAt) {
        Instant createdAt = NOW.minusSeconds(3600);

        return SessaoAutenticacao.criar(
            usuario.getId(),
            HASH_ATUAL,
            createdAt,
            Duration.between(createdAt, expiresAt)
        );
    }

    private void prepararRenovacao(SessaoAutenticacao sessao) {
        when(refreshTokens.hash("refresh-atual")).thenReturn(HASH_ATUAL);
        when(sessoes.findByRefreshTokenHash(HASH_ATUAL))
            .thenReturn(Optional.of(sessao));
        when(usuarios.findById(usuario.getId()))
            .thenReturn(Optional.of(usuario));
        when(refreshTokens.generate()).thenReturn("novo-refresh");
        when(refreshTokens.hash("novo-refresh")).thenReturn(HASH_NOVO);
    }
}