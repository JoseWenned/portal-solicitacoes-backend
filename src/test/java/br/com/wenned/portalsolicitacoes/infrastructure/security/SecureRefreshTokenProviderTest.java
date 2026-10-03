package br.com.wenned.portalsolicitacoes.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SecureRefreshTokenProviderTest {

    private final SecureRefreshTokenProvider provider =
        new SecureRefreshTokenProvider();

    @Test
    void deveGerarTokensDistintosComTrintaEDoisBytes() {
        String primeiro = provider.generate();
        String segundo = provider.generate();

        assertThat(primeiro).isNotEqualTo(segundo);
        assertThat(primeiro).matches("^[A-Za-z0-9_-]{43}$");
        assertThat(Base64.getUrlDecoder().decode(primeiro)).hasSize(32);
    }

    @Test
    void deveProduzirHashSha256Conhecido() {
        assertThat(provider.hash("abc")).isEqualTo(
            "ba7816bf8f01cfea414140de5dae2223"
                + "b00361a396177a9cb410ff61f20015ad"
        );
    }
}