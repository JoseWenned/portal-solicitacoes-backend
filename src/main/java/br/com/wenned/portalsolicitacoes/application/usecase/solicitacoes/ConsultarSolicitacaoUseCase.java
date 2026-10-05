package br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.dto.solicitacoes.ConsultarSolicitacaoResultadoDTO;
import br.com.wenned.portalsolicitacoes.application.port.out.solicitacoes.SolicitacaoRepository;
import br.com.wenned.portalsolicitacoes.domain.exception.solicitacoes.SolicitacaoNaoEncontradaException;

import java.util.Objects;
import java.util.UUID;

public final class ConsultarSolicitacaoUseCase {

    private final SolicitacaoRepository repository;

    public ConsultarSolicitacaoUseCase(SolicitacaoRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public ConsultarSolicitacaoResultadoDTO executar(
        UUID id,
        UUID solicitanteId
    ) {
        Objects.requireNonNull(id, "Identificador obrigatório.");
        Objects.requireNonNull(solicitanteId, "Solicitante obrigatório.");

        var solicitacao = repository.consultarPorIdEProprietario(
            id,
            solicitanteId
        ).orElseThrow(SolicitacaoNaoEncontradaException::new);

        return new ConsultarSolicitacaoResultadoDTO(
            solicitacao.getId(),
            solicitacao.getCodigo(),
            solicitacao.getTitulo(),
            solicitacao.getDescricao(),
            solicitacao.getCategoria(),
            solicitacao.getStatus(),
            solicitacao.getSolicitanteId(),
            solicitacao.getCreatedAt(),
            solicitacao.getUpdatedAt()
        );
    }
}