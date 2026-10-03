package br.com.wenned.portalsolicitacoes.infrastructure.configuration;

import br.com.wenned.portalsolicitacoes.application.port.out.security.PasswordHasher;
import br.com.wenned.portalsolicitacoes.application.port.out.usuarios.UsuarioRepository;
import br.com.wenned.portalsolicitacoes.application.usecase.usuarios.CadastrarUsuarioUseCase;
import br.com.wenned.portalsolicitacoes.infrastructure.security.BCryptPasswordHasher;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class UsuarioConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public PasswordHasher passwordHasher(PasswordEncoder encoder) {
        return new BCryptPasswordHasher(encoder);
    }

    @Bean
    public CadastrarUsuarioUseCase cadastrarUsuarioUseCase(
        UsuarioRepository repository,
        PasswordHasher hasher,
        Clock clock
    ) {
        return new CadastrarUsuarioUseCase(repository, hasher, clock);
    }
}