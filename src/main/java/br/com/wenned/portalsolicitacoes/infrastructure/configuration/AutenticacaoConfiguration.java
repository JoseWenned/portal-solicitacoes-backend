package br.com.wenned.portalsolicitacoes.infrastructure.configuration;

import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.AccessTokenProvider;
import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.RefreshTokenProvider;
import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.SessaoAutenticacaoRepository;
import br.com.wenned.portalsolicitacoes.application.port.out.security.PasswordHasher;
import br.com.wenned.portalsolicitacoes.application.port.out.usuarios.UsuarioRepository;
import br.com.wenned.portalsolicitacoes.application.usecase.autenticacao.AutenticacaoUseCase;
import br.com.wenned.portalsolicitacoes.infrastructure.security.JwtAccessTokenProvider;
import br.com.wenned.portalsolicitacoes.infrastructure.security.SecureRefreshTokenProvider;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
public class AutenticacaoConfiguration {

    @Bean
    public SecretKey jwtSigningKey(
        @Value("${portal.security.jwt.secret-base64}") String encoded
    ) {
        byte[] bytes;

        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "A chave JWT deve estar codificada em Base64."
            );
        }

        if (bytes.length < 32) {
            throw new IllegalArgumentException(
                "A chave JWT deve conter pelo menos 32 bytes."
            );
        }

        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(
            new ImmutableSecret<>(jwtSigningKey)
        );
    }

    @Bean
    public AccessTokenProvider accessTokenProvider(
        JwtEncoder encoder,
        @Value("${portal.security.jwt.issuer}") String issuer,
        @Value("${portal.security.jwt.audience}") String audience
    ) {
        return new JwtAccessTokenProvider(encoder, issuer, audience);
    }

    @Bean
    public RefreshTokenProvider refreshTokenProvider() {
        return new SecureRefreshTokenProvider();
    }

    @Bean
    public AutenticacaoUseCase autenticacaoUseCase(
        UsuarioRepository usuarios,
        SessaoAutenticacaoRepository sessoes,
        PasswordHasher hasher,
        PasswordEncoder encoder,
        RefreshTokenProvider refreshTokens,
        AccessTokenProvider accessTokens,
        Clock clock,
        @Value("${portal.security.access-token-validade}")
        Duration accessValidade,
        @Value("${portal.security.sessao-validade}")
        Duration sessaoValidade
    ) {
        String dummyHash = encoder.encode(
            UUID.randomUUID().toString()
        );

        return new AutenticacaoUseCase(
            usuarios,
            sessoes,
            hasher,
            refreshTokens,
            accessTokens,
            clock,
            accessValidade,
            sessaoValidade,
            dummyHash
        );
    }
}