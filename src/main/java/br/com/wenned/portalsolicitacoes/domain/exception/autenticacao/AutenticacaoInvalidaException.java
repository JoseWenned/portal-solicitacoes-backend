package br.com.wenned.portalsolicitacoes.domain.exception.autenticacao;

public class AutenticacaoInvalidaException extends RuntimeException {

    public AutenticacaoInvalidaException() {
        super("Credenciais inválidas ou sessão indisponível.");
    }
}