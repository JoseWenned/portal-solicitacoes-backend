package br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.port.out.solicitacoes.SolicitacaoRepository;
import br.com.wenned.portalsolicitacoes.domain.exception.solicitacoes.OperacaoSolicitacaoInvalidaException;
import br.com.wenned.portalsolicitacoes.domain.exception.solicitacoes.SolicitacaoNaoEncontradaException;

import java.util.Objects;
import java.util.UUID;

public final class ExcluirSolicitacaoUseCase {

    private final SolicitacaoRepository repository;

    public ExcluirSolicitacaoUseCase(SolicitacaoRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public void executar(UUID id, UUID solicitanteId) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(solicitanteId);

        var solicitacao = repository.consultarPorIdEProprietario(
            id,
            solicitanteId
        ).orElseThrow(SolicitacaoNaoEncontradaException::new);

        solicitacao.validarExclusao();

        if (!repository.excluir(
            id,
            solicitanteId,
            solicitacao.getVersion()
        )) {
            throw new OperacaoSolicitacaoInvalidaException(
                "A solicitação foi alterada ou excluída. Atualize os dados e tente novamente."
            );
        }
    }
}