package br.com.wenned.portalsolicitacoes.infrastructure.persistence.adapter.autenticacao;

import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.SessaoAutenticacaoRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.autenticacao.SessaoAutenticacao;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.mapper.autenticacao.SessaoAutenticacaoMapper;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.model.autenticacao.SessaoAutenticacaoModel;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.repository.autenticacao.SessaoAutenticacaoRepositorioJPA;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SessaoAutenticacaoRepositoryAdapter implements SessaoAutenticacaoRepository {

    private final SessaoAutenticacaoRepositorioJPA repository;
    private final EntityManager entityManager;

    public SessaoAutenticacaoRepositoryAdapter(
        SessaoAutenticacaoRepositorioJPA repository,
        EntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public SessaoAutenticacao create(SessaoAutenticacao sessao) {
        if (sessao.getVersion() != 0 || sessao.getRevokedAt() != null) {
            throw new IllegalArgumentException(
                "A criação exige uma sessão nova e não revogada."
            );
        }

        SessaoAutenticacaoModel model =
            SessaoAutenticacaoMapper.toModel(sessao);

        entityManager.persist(model);
        entityManager.flush();

        return SessaoAutenticacaoMapper.toDomain(model);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SessaoAutenticacao> findById(UUID id) {
        return repository.findById(id)
            .map(SessaoAutenticacaoMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SessaoAutenticacao> findByRefreshTokenHash(
        String hash
    ) {
        return repository.findByRefreshTokenHash(hash)
            .map(SessaoAutenticacaoMapper::toDomain);
    }

    @Override
    @Transactional
    public boolean rotateRefreshToken(
        UUID sessaoId,
        long expectedVersion,
        String expectedHash,
        String newHash,
        Instant now
    ) {
        Objects.requireNonNull(sessaoId);
        Objects.requireNonNull(now);
        validarHash(expectedHash);
        validarHash(newHash);

        if (expectedVersion < 0 || expectedHash.equals(newHash)) {
            throw new IllegalArgumentException(
                "Os dados da rotação são inválidos."
            );
        }

        return repository.rotateRefreshToken(
            sessaoId,
            expectedVersion,
            expectedHash,
            newHash,
            now
        ) == 1;
    }

    @Override
    @Transactional
    public void revoke(UUID sessaoId, Instant now) {
        Objects.requireNonNull(sessaoId);
        Objects.requireNonNull(now);

        repository.revoke(sessaoId, now);
    }

    private void validarHash(String hash) {
        if (hash == null || !hash.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                "O hash do refresh token é inválido."
            );
        }
    }
}