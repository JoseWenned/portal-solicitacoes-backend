package br.com.wenned.portalsolicitacoes.application.usecase.autenticacao;

import br.com.wenned.portalsolicitacoes.application.dto.autenticacao.AutenticacaoResultadoDTO;
import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.AccessTokenProvider;
import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.RefreshTokenProvider;
import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.SessaoAutenticacaoRepository;
import br.com.wenned.portalsolicitacoes.application.port.out.security.PasswordHasher;
import br.com.wenned.portalsolicitacoes.application.port.out.usuarios.UsuarioRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.autenticacao.SessaoAutenticacao;
import br.com.wenned.portalsolicitacoes.domain.entity.usuarios.Usuario;
import br.com.wenned.portalsolicitacoes.domain.exception.autenticacao.AutenticacaoInvalidaException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public class AutenticacaoUseCase {

    private final UsuarioRepository usuarios;
    private final SessaoAutenticacaoRepository sessoes;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenProvider refreshTokens;
    private final AccessTokenProvider accessTokens;
    private final Clock clock;
    private final Duration accessTokenValidade;
    private final Duration sessaoValidade;
    private final String dummyPasswordHash;

    public AutenticacaoUseCase(
        UsuarioRepository usuarios,
        SessaoAutenticacaoRepository sessoes,
        PasswordHasher passwordHasher,
        RefreshTokenProvider refreshTokens,
        AccessTokenProvider accessTokens,
        Clock clock,
        Duration accessTokenValidade,
        Duration sessaoValidade,
        String dummyPasswordHash
    ) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.sessoes = Objects.requireNonNull(sessoes);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.refreshTokens = Objects.requireNonNull(refreshTokens);
        this.accessTokens = Objects.requireNonNull(accessTokens);
        this.clock = Objects.requireNonNull(clock);
        this.accessTokenValidade = validarDuracao(accessTokenValidade);
        this.sessaoValidade = validarDuracao(sessaoValidade);

        if (dummyPasswordHash == null || dummyPasswordHash.isBlank()) {
            throw new IllegalArgumentException(
                "O hash de comparação auxiliar é obrigatório."
            );
        }

        this.dummyPasswordHash = dummyPasswordHash;
    }

    public AutenticacaoResultadoDTO login(String email, String password) {
        if (email == null || email.isBlank() || !senhaPossivel(password)) {
            throw new AutenticacaoInvalidaException();
        }

        String normalizedEmail =
            email.strip().toLowerCase(Locale.ROOT);

        var usuarioEncontrado = usuarios.findByEmail(normalizedEmail);

        String hash = usuarioEncontrado
            .map(Usuario::getPasswordHash)
            .orElse(dummyPasswordHash);

        boolean senhaCorreta = passwordHasher.matches(password, hash);

        if (usuarioEncontrado.isEmpty() || !senhaCorreta) {
            throw new AutenticacaoInvalidaException();
        }

        Usuario usuario = usuarioEncontrado.orElseThrow();
        Instant now = clock.instant();

        String refreshToken = refreshTokens.generate();

        SessaoAutenticacao sessao = SessaoAutenticacao.criar(
            usuario.getId(),
            refreshTokens.hash(refreshToken),
            now,
            sessaoValidade
        );

        AutenticacaoResultadoDTO resultado =
            gerarResultado(usuario, sessao, refreshToken, now);

        sessoes.create(sessao);

        return resultado;
    }

    public AutenticacaoResultadoDTO renovar(String refreshToken) {
        validarRefreshToken(refreshToken);

        Instant now = clock.instant();
        String currentHash = refreshTokens.hash(refreshToken);

        SessaoAutenticacao sessao = sessoes
            .findByRefreshTokenHash(currentHash)
            .orElseThrow(AutenticacaoInvalidaException::new);

        if (!sessao.estaAtiva(now)) {
            throw new AutenticacaoInvalidaException();
        }

        Usuario usuario = usuarios.findById(sessao.getUsuarioId())
            .orElseThrow(AutenticacaoInvalidaException::new);

        String novoRefreshToken = refreshTokens.generate();
        String novoHash = refreshTokens.hash(novoRefreshToken);

        // Aplica as regras de domínio sem alterar a expiração.
        SessaoAutenticacao atualizada =
            sessao.rotacionarRefreshToken(novoHash, now);

        AutenticacaoResultadoDTO resultado = gerarResultado(
            usuario,
            atualizada,
            novoRefreshToken,
            now
        );

        // Usa um horário atualizado para não renovar uma sessão
        // que expirou durante a preparação da resposta.
        boolean alterou = sessoes.rotateRefreshToken(
            sessao.getId(),
            sessao.getVersion(),
            currentHash,
            novoHash,
            clock.instant()
        );

        if (!alterou) {
            throw new AutenticacaoInvalidaException();
        }

        return resultado;
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        // Token desconhecido também resulta em logout sem erro.
        sessoes.findByRefreshTokenHash(refreshTokens.hash(refreshToken))
            .ifPresent(sessao ->
                sessoes.revoke(sessao.getId(), clock.instant())
            );
    }

    private AutenticacaoResultadoDTO gerarResultado(
        Usuario usuario,
        SessaoAutenticacao sessao,
        String refreshToken,
        Instant now
    ) {
        Instant expiresAt = now.plus(accessTokenValidade);

        if (expiresAt.isAfter(sessao.getExpiresAt())) {
            expiresAt = sessao.getExpiresAt();
        }

        long expiresIn = Duration.between(now, expiresAt).getSeconds();

        if (expiresIn <= 0) {
            throw new AutenticacaoInvalidaException();
        }

        String jwt = accessTokens.generate(
            usuario.getId(),
            sessao.getId(),
            now,
            expiresAt
        );

        return new AutenticacaoResultadoDTO(
            jwt,
            expiresIn,
            refreshToken,
            sessao.getExpiresAt(),
            new AutenticacaoResultadoDTO.UsuarioAutenticado(
                usuario.getId(),
                usuario.getName(),
                usuario.getEmail()
            )
        );
    }

    private boolean senhaPossivel(String password) {
        return password != null
            && !password.isBlank()
            && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private void validarRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new AutenticacaoInvalidaException();
        }
    }

    private Duration validarDuracao(Duration duracao) {
        if (duracao == null || duracao.isZero() || duracao.isNegative()) {
            throw new IllegalArgumentException(
                "A validade deve ser positiva."
            );
        }

        return duracao;
    }
}