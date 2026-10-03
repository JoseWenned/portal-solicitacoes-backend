package br.com.wenned.portalsolicitacoes.application.port.out.autenticacao;

public interface RefreshTokenProvider {

    String generate();

    String hash(String rawToken);
}