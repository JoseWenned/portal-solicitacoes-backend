package br.com.wenned.portalsolicitacoes.application.port.out.autenticacao;

import br.com.wenned.portalsolicitacoes.domain.entity.autenticacao.SessaoAutenticacao;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SessaoAutenticacaoRepository {

    SessaoAutenticacao create(SessaoAutenticacao sessao);

    Optional<SessaoAutenticacao> findById(UUID id);

    Optional<SessaoAutenticacao> findByRefreshTokenHash(String hash);

    boolean rotateRefreshToken(
        UUID sessaoId,
        long expectedVersion,
        String expectedHash,
        String newHash,
        Instant now
    );

    void revoke(UUID sessaoId, Instant now);
}