package br.com.wenned.portalsolicitacoes.domain.exception.solicitacoes;

public class SolicitacaoNaoEncontradaException extends RuntimeException {

    public SolicitacaoNaoEncontradaException() {
        super("Solicitação não encontrada.");
    }
}