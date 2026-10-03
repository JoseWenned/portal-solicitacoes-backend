package br.com.wenned.portalsolicitacoes.infrastructure.security;

import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.RefreshTokenProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

public final class SecureRefreshTokenProvider implements RefreshTokenProvider {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }

    @Override
    public String hash(String rawToken) {
        Objects.requireNonNull(rawToken);

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 não está disponível.",
                exception
            );
        }
    }
}