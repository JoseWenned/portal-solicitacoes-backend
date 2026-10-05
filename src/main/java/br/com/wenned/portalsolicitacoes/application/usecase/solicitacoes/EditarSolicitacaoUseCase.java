package br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.port.out.solicitacoes.SolicitacaoRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.CategoriaSolicitacao;
import br.com.wenned.portalsolicitacoes.domain.exception.solicitacoes.OperacaoSolicitacaoInvalidaException;
import br.com.wenned.portalsolicitacoes.domain.exception.solicitacoes.SolicitacaoNaoEncontradaException;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class EditarSolicitacaoUseCase {

    private final SolicitacaoRepository repository;
    private final Clock clock;

    public EditarSolicitacaoUseCase(
        SolicitacaoRepository repository,
        Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public void executar(
        UUID id,
        UUID solicitanteId,
        String titulo,
        String descricao,
        CategoriaSolicitacao categoria
    ) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(solicitanteId);

        var original = repository.consultarPorIdEProprietario(
            id,
            solicitanteId
        ).orElseThrow(SolicitacaoNaoEncontradaException::new);

        var editada = original.editar(
            titulo,
            descricao,
            categoria,
            clock.instant()
        );

        if (!repository.editar(editada)) {
            throw new OperacaoSolicitacaoInvalidaException(
                "A solicitação foi alterada ou excluída. Atualize os dados e tente novamente."
            );
        }
    }
}