package br.com.wenned.portalsolicitacoes.infrastructure.security;

import br.com.wenned.portalsolicitacoes.application.port.out.security.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;

public class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    public BCryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }
}