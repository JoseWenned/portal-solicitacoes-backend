package br.com.wenned.portalsolicitacoes.infrastructure.persistence.adapter.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.port.out.solicitacoes.SolicitacaoRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.Solicitacao;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.mapper.solicitacoes.SolicitacaoMapper;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.model.solicitacoes.SolicitacaoModel;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Repository
public class SolicitacaoRepositoryAdapter implements SolicitacaoRepository {

    private final EntityManager entityManager;

    public SolicitacaoRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager);
    }

    @Override
    @Transactional
    public Solicitacao criar(Solicitacao solicitacao) {
        Objects.requireNonNull(solicitacao);

        if (solicitacao.getCodigo() != null
            || solicitacao.getVersion() != 0) {
            throw new IllegalArgumentException(
                "A criação exige uma solicitação nova, sem código e com versão zero."
            );
        }

        SolicitacaoModel model = SolicitacaoMapper.toModel(solicitacao);

        entityManager.persist(model);
        entityManager.flush();
        entityManager.refresh(model);

        return SolicitacaoMapper.toDomain(model);
    }
}