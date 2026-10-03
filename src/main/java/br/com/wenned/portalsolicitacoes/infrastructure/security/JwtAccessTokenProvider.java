package br.com.wenned.portalsolicitacoes.infrastructure.security;

import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.AccessTokenProvider;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class JwtAccessTokenProvider implements AccessTokenProvider {

    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;

    public JwtAccessTokenProvider(
        JwtEncoder encoder,
        String issuer,
        String audience
    ) {
        this.encoder = Objects.requireNonNull(encoder);

        if (issuer == null || issuer.isBlank()
                || audience == null || audience.isBlank()) {
            throw new IllegalArgumentException(
                "Emissor e destinatário são obrigatórios."
            );
        }

        this.issuer = issuer;
        this.audience = audience;
    }

    @Override
    public String generate(
        UUID usuarioId,
        UUID sessaoId,
        Instant issuedAt,
        Instant expiresAt
    ) {
        Objects.requireNonNull(usuarioId);
        Objects.requireNonNull(sessaoId);
        Objects.requireNonNull(issuedAt);
        Objects.requireNonNull(expiresAt);

        if (expiresAt.getEpochSecond() <= issuedAt.getEpochSecond()) {
            throw new IllegalArgumentException(
                "O JWT deve possuir validade de pelo menos um segundo."
            );
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(List.of(audience))
            .subject(usuarioId.toString())
            .id(UUID.randomUUID().toString())
            .issuedAt(issuedAt)
            .notBefore(issuedAt)
            .expiresAt(expiresAt)
            .claim("sid", sessaoId.toString())
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
            .type("JWT")
            .build();

        return encoder.encode(
            JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}