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

    @Override
    @Transactional
    public boolean editar(Solicitacao solicitacao) {
        Objects.requireNonNull(solicitacao);

        if (solicitacao.getStatus() != StatusSolicitacao.ABERTO) {
            throw new IllegalArgumentException(
                "A persistência da edição exige status ABERTO."
            );
        }

        return repositoryJPA.editarCondicionalmente(
            solicitacao.getId(),
            solicitacao.getSolicitanteId(),
            solicitacao.getVersion(),
            StatusSolicitacao.ABERTO,
            solicitacao.getTitulo(),
            solicitacao.getDescricao(),
            solicitacao.getCategoria(),
            solicitacao.getUpdatedAt()
        ) == 1;
    }

    @Override
    @Transactional
    public boolean alterarStatus(
        Solicitacao solicitacao,
        StatusSolicitacao statusAnterior
    ) {
        Objects.requireNonNull(solicitacao);
        Objects.requireNonNull(statusAnterior);

        boolean transicaoPermitida =
            (statusAnterior == StatusSolicitacao.ABERTO
                && solicitacao.getStatus() == StatusSolicitacao.EM_ATENDIMENTO)
            || (statusAnterior == StatusSolicitacao.EM_ATENDIMENTO
                && solicitacao.getStatus() == StatusSolicitacao.CONCLUIDO);

        if (!transicaoPermitida) {
            throw new IllegalArgumentException(
                "Transição inválida para persistência."
            );
        }

        return repositoryJPA.alterarStatusCondicionalmente(
            solicitacao.getId(),
            solicitacao.getSolicitanteId(),
            solicitacao.getVersion(),
            statusAnterior,
            solicitacao.getStatus(),
            solicitacao.getUpdatedAt()
        ) == 1;
    }

    @Override
    @Transactional
    public boolean excluir(
        UUID id,
        UUID solicitanteId,
        long versaoEsperada
    ) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(solicitanteId);

        if (versaoEsperada < 0) {
            throw new IllegalArgumentException(
                "Versão esperada não pode ser negativa."
            );
        }

        return repositoryJPA.excluirCondicionalmente(
            id,
            solicitanteId,
            versaoEsperada,
            StatusSolicitacao.ABERTO
        ) == 1;
    }
}