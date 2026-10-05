package br.com.wenned.portalsolicitacoes.application.port.out.solicitacoes;

import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.Solicitacao;

public interface SolicitacaoRepository {

    Solicitacao criar(Solicitacao solicitacao);
}