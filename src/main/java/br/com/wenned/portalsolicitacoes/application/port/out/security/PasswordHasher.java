package br.com.wenned.portalsolicitacoes.application.port.out.security;

public interface PasswordHasher {

    String hash(String rawPassword);
}