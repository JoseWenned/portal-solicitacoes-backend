package br.com.wenned.portalsolicitacoes.domain.exception.usuarios;

public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException() {
        super("Já existe um usuário com esse e-mail.");
    }
}