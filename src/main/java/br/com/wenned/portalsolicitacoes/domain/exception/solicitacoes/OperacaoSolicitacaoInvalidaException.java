package br.com.wenned.portalsolicitacoes.domain.exception.solicitacoes;

public class OperacaoSolicitacaoInvalidaException extends RuntimeException {

    public OperacaoSolicitacaoInvalidaException(String message) {
        super(message);
    }
}