package br.com.wenned.portalsolicitacoes.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAccessTokenProviderTest {

    private static final String ISSUER = "portal-solicitacoes";
    private static final String AUDIENCE = "portal-solicitacoes-api";

    @Test
    void deveAssinarTokenComIdentificacaoDeUsuarioESessao() {
        var key = novaChave();

        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var provider = new JwtAccessTokenProvider(
            encoder, ISSUER, AUDIENCE
        );

        var decoder = NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

        decoder.setJwtValidator(
            JwtValidators.createDefaultWithIssuer(ISSUER)
        );

        UUID usuarioId = UUID.randomUUID();
        UUID sessaoId = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(
            java.time.temporal.ChronoUnit.SECONDS
        );

        var jwt = decoder.decode(provider.generate(
            usuarioId,
            sessaoId,
            now,
            now.plusSeconds(900)
        ));

        assertThat(jwt.getSubject()).isEqualTo(usuarioId.toString());
        assertThat(jwt.getClaimAsString("sid"))
            .isEqualTo(sessaoId.toString());
        assertThat(jwt.getAudience()).containsExactly(AUDIENCE);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plusSeconds(900));

        assertThat(jwt.getClaims())
            .doesNotContainKeys("password", "passwordHash", "refreshToken");
    }

    @Test
    void deveRecusarTokenAssinadoComOutraChave() {
        var encoder = new NimbusJwtEncoder(
            new ImmutableSecret<>(novaChave())
        );

        var provider = new JwtAccessTokenProvider(
            encoder, ISSUER, AUDIENCE
        );

        var decoder = NimbusJwtDecoder.withSecretKey(novaChave())
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

        Instant now = Instant.now();

        String token = provider.generate(
            UUID.randomUUID(),
            UUID.randomUUID(),
            now,
            now.plusSeconds(900)
        );

        assertThatThrownBy(() -> decoder.decode(token))
            .isInstanceOf(JwtException.class);
    }

    private SecretKeySpec novaChave() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);

        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}