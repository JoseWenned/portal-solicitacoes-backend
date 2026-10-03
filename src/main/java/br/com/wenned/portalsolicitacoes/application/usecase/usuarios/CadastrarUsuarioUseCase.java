package br.com.wenned.portalsolicitacoes.application.usecase.usuarios;

import br.com.wenned.portalsolicitacoes.application.port.out.security.PasswordHasher;
import br.com.wenned.portalsolicitacoes.application.port.out.usuarios.UsuarioRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.usuarios.Usuario;
import br.com.wenned.portalsolicitacoes.domain.exception.usuarios.EmailJaCadastradoException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class CadastrarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public CadastrarUsuarioUseCase(
        UsuarioRepository usuarioRepository,
        PasswordHasher passwordHasher,
        Clock clock
    ) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.clock = Objects.requireNonNull(clock);
    }

    public Resultado execute(String name, String email, String password) {
        String normalizedName = Usuario.normalizarNome(name);
        String normalizedEmail = Usuario.normalizarEmail(email);

        validarSenha(password);

        if (usuarioRepository.existsByEmail(normalizedEmail)) {
            throw new EmailJaCadastradoException();
        }

        Usuario usuario = Usuario.criar(
            normalizedName,
            normalizedEmail,
            passwordHasher.hash(password),
            Instant.now(clock)
        );

        Usuario saved = usuarioRepository.save(usuario);

        return new Resultado(
            saved.getId(),
            saved.getName(),
            saved.getEmail(),
            saved.getCreatedAt()
        );
    }

    private void validarSenha(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("A senha é obrigatória.");
        }

        int length = password.codePointCount(0, password.length());

        if (length < 8) {
            throw new IllegalArgumentException(
                "A senha deve ter pelo menos 8 caracteres."
            );
        }

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                "A senha deve ter no máximo 72 bytes em UTF-8."
            );
        }
    }

    public record Resultado(
        UUID id,
        String name,
        String email,
        Instant createdAt
    ) {}
}