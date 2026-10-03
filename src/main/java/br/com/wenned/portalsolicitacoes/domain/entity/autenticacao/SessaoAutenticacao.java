package br.com.wenned.portalsolicitacoes.domain.entity.autenticacao;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class SessaoAutenticacao {

    private final UUID id;
    private final UUID usuarioId;
    private final String refreshTokenHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant revokedAt;
    private final long version;

    private SessaoAutenticacao(
            UUID id,
            UUID usuarioId,
            String refreshTokenHash,
            Instant createdAt,
            Instant expiresAt,
            Instant revokedAt,
            long version
    ) {
        this.id = Objects.requireNonNull(id);
        this.usuarioId = Objects.requireNonNull(usuarioId);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);

        if (refreshTokenHash == null || !refreshTokenHash.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                "O hash do refresh token é inválido."
            );
        }

        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException(
                "A expiração deve ser posterior à criação."
            );
        }

        if (revokedAt != null && revokedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                "A revogação não pode anteceder a criação."
            );
        }

        if (version < 0) {
            throw new IllegalArgumentException(
                "A versão não pode ser negativa."
            );
        }

        this.refreshTokenHash = refreshTokenHash;
        this.revokedAt = revokedAt;
        this.version = version;
    }

    public static SessaoAutenticacao criar(
        UUID usuarioId,
        String refreshTokenHash,
        Instant now,
        Duration validade
    ) {
        Objects.requireNonNull(validade);

        if (validade.isZero() || validade.isNegative()) {
            throw new IllegalArgumentException(
                "A validade deve ser positiva."
            );
        }

        return new SessaoAutenticacao(
            UUID.randomUUID(),
            usuarioId,
            refreshTokenHash,
            now,
            now.plus(validade),
            null,
            0
        );
    }

    public static SessaoAutenticacao reconstituir(
        UUID id,
        UUID usuarioId,
        String refreshTokenHash,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt,
        long version
    ) {
        return new SessaoAutenticacao(
            id,
            usuarioId,
            refreshTokenHash,
            createdAt,
            expiresAt,
            revokedAt,
            version
        );
    }

    public boolean estaAtiva(Instant now) {
        Objects.requireNonNull(now);

        return revokedAt == null
            && !now.isBefore(createdAt)
            && now.isBefore(expiresAt);
    }

    public SessaoAutenticacao rotacionarRefreshToken(
        String novoHash,
        Instant now
    ) {
        if (!estaAtiva(now)) {
            throw new IllegalStateException("A sessão não está ativa.");
        }

        if (refreshTokenHash.equals(novoHash)) {
            throw new IllegalArgumentException(
                "A rotação deve substituir o refresh token."
            );
        }

        return new SessaoAutenticacao(
            id,
            usuarioId,
            novoHash,
            createdAt,
            expiresAt,
            revokedAt,
            version
        );
    }

    public SessaoAutenticacao revogar(Instant now) {
        Objects.requireNonNull(now);

        if (now.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                "A revogação não pode anteceder a criação."
            );
        }

        if (revokedAt != null) {
            return this;
        }

        return new SessaoAutenticacao(
            id,
            usuarioId,
            refreshTokenHash,
            createdAt,
            expiresAt,
            now,
            version
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        return other instanceof SessaoAutenticacao sessao && id.equals(sessao.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "SessaoAutenticacao[id=" + id + "]";
    }
}