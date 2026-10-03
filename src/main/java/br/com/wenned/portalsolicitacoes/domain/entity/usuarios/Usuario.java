package br.com.wenned.portalsolicitacoes.domain.entity.usuarios;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class Usuario {

    private final UUID id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final Instant createdAt;

    private Usuario(
        UUID id,
        String name,
        String email,
        String passwordHash,
        Instant createdAt
    ) {
        this.id = Objects.requireNonNull(
            id, "O identificador é obrigatório."
        );
        this.name = normalizarNome(name);
        this.email = normalizarEmail(email);

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException(
                "O hash da senha é obrigatório."
            );
        }

        this.passwordHash = passwordHash;
        this.createdAt = Objects.requireNonNull(
            createdAt, "A data de criação é obrigatória."
        );
    }

    public static Usuario criar(
        String name,
        String email,
        String passwordHash,
        Instant createdAt
    ) {
        return new Usuario(
            UUID.randomUUID(),
            name,
            email,
            passwordHash,
            createdAt
        );
    }

    public static Usuario reconstituir(
        UUID id,
        String name,
        String email,
        String passwordHash,
        Instant createdAt
    ) {
        return new Usuario(id, name, email, passwordHash, createdAt);
    }

    public static String normalizarNome(String name) {
        if (name == null) {
            throw new IllegalArgumentException("O nome é obrigatório.");
        }

        String normalized = name.strip();
        int length = normalized.codePointCount(0, normalized.length());

        if (length < 3 || length > 100) {
            throw new IllegalArgumentException(
                "O nome deve ter entre 3 e 100 caracteres."
            );
        }

        return normalized;
    }

    public static String normalizarEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("O e-mail é obrigatório.");
        }

        String normalized = email.strip().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()
                || normalized.length() > 254
                || !normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("O e-mail é inválido.");
        }

        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Usuario usuario)) {
            return false;
        }

        return id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Usuario[id=" + id + "]";
    }
}