package br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.dto.solicitacoes.CriarSolicitacaoResultadoDTO;
import br.com.wenned.portalsolicitacoes.application.port.out.solicitacoes.SolicitacaoRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.CategoriaSolicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.Solicitacao;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class CriarSolicitacaoUseCase {

    private final SolicitacaoRepository repository;
    private final Clock clock;

    public CriarSolicitacaoUseCase(
        SolicitacaoRepository repository,
        Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public CriarSolicitacaoResultadoDTO executar(
        UUID solicitanteId,
        String titulo,
        String descricao,
        CategoriaSolicitacao categoria
    ) {
        Solicitacao solicitacao = Solicitacao.criar(
            titulo,
            descricao,
            categoria,
            solicitanteId,
            clock.instant()
        );

        Solicitacao persistida = repository.criar(solicitacao);

        return new CriarSolicitacaoResultadoDTO(
            persistida.getId(),
            persistida.getCodigo(),
            persistida.getTitulo(),
            persistida.getDescricao(),
            persistida.getCategoria(),
            persistida.getStatus(),
            persistida.getSolicitanteId(),
            persistida.getCreatedAt(),
            persistida.getUpdatedAt()
        );
    }
}