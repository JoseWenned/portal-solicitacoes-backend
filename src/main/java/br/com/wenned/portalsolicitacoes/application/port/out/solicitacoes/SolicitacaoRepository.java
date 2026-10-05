package br.com.wenned.portalsolicitacoes.application.port.out.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.dto.result.PaginaResultadoDTO;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.CategoriaSolicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.Solicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.StatusSolicitacao;

import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoRepository {

    Solicitacao criar(Solicitacao solicitacao);

    Optional<Solicitacao> consultarPorIdEProprietario(
        UUID id,
        UUID solicitanteId
    );

    PaginaResultadoDTO<Solicitacao> listarPorProprietario(
        UUID solicitanteId,
        StatusSolicitacao status,
        CategoriaSolicitacao categoria,
        int page,
        int size
    );

    boolean editar(Solicitacao solicitacao);

    boolean alterarStatus(
        Solicitacao solicitacao,
        StatusSolicitacao statusAnterior
    );

    boolean excluir(
        UUID id,
        UUID solicitanteId,
        long versaoEsperada
    );
}