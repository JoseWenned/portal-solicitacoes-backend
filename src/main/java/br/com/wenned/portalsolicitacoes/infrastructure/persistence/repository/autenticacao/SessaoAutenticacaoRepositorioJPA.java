package br.com.wenned.portalsolicitacoes.infrastructure.persistence.repository.autenticacao;

import br.com.wenned.portalsolicitacoes.infrastructure.persistence.model.autenticacao.SessaoAutenticacaoModel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SessaoAutenticacaoRepositorioJPA extends JpaRepository<SessaoAutenticacaoModel, UUID> {

    Optional<SessaoAutenticacaoModel> findByRefreshTokenHash(
        String refreshTokenHash
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SessaoAutenticacaoModel s
        SET s.refreshTokenHash = :newHash,
            s.version = s.version + 1
        WHERE s.id = :id
            AND s.version = :expectedVersion
            AND s.refreshTokenHash = :expectedHash
            AND s.revokedAt IS NULL
            AND s.createdAt <= :now
            AND s.expiresAt > :now
        """)
    int rotateRefreshToken(
        @Param("id") UUID id,
        @Param("expectedVersion") long expectedVersion,
        @Param("expectedHash") String expectedHash,
        @Param("newHash") String newHash,
        @Param("now") Instant now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SessaoAutenticacaoModel s
        SET s.revokedAt = :now,
            s.version = s.version + 1
        WHERE s.id = :id
            AND s.revokedAt IS NULL
            AND s.createdAt <= :now
        """)
    int revoke(
        @Param("id") UUID id,
        @Param("now") Instant now
    );
}