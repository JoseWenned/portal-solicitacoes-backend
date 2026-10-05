package br.com.wenned.portalsolicitacoes.infrastructure.persistence.adapter.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.dto.result.PaginaResultadoDTO;
import br.com.wenned.portalsolicitacoes.application.port.out.solicitacoes.SolicitacaoRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.CategoriaSolicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.Solicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.StatusSolicitacao;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.mapper.solicitacoes.SolicitacaoMapper;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.model.solicitacoes.SolicitacaoModel;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.repository.solicitacoes.SolicitacaoRepositorioJPA;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SolicitacaoRepositoryAdapter implements SolicitacaoRepository {

    private final EntityManager entityManager;
    private final SolicitacaoRepositorioJPA repositoryJPA;

    public SolicitacaoRepositoryAdapter(
        EntityManager entityManager,
        SolicitacaoRepositorioJPA repositoryJPA
    ) {
        this.entityManager = Objects.requireNonNull(entityManager);
        this.repositoryJPA = Objects.requireNonNull(repositoryJPA);
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

    @Override
    @Transactional(readOnly = true)
    public Optional<Solicitacao> consultarPorIdEProprietario(
        UUID id,
        UUID solicitanteId
    ) {
        return repositoryJPA.findByIdAndSolicitanteId(id, solicitanteId)
            .map(SolicitacaoMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResultadoDTO<Solicitacao> listarPorProprietario(
        UUID solicitanteId,
        StatusSolicitacao status,
        CategoriaSolicitacao categoria,
        int page,
        int size
    ) {
        Objects.requireNonNull(solicitanteId, "Solicitante obrigatório.");

        Specification<SolicitacaoModel> filtros = (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();

            predicates.add(
                builder.equal(root.get("solicitanteId"), solicitanteId)
            );

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            if (categoria != null) {
                predicates.add(
                    builder.equal(root.get("categoria"), categoria)
                );
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };

        var pageable = PageRequest.of(
            page,
            size,
            Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("codigo")
            )
        );

        var resultado = repositoryJPA.findAll(filtros, pageable);

        var content = resultado.getContent().stream()
            .map(SolicitacaoMapper::toDomain)
            .toList();

        return new PaginaResultadoDTO<>(
            content,
            resultado.getNumber(),
            resultado.getSize(),
            resultado.getTotalElements(),
            resultado.getTotalPages()
        );
    }
}