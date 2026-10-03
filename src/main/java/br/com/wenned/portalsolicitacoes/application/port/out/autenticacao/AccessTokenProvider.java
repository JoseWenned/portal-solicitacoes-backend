package br.com.wenned.portalsolicitacoes.application.port.out.autenticacao;

import java.time.Instant;
import java.util.UUID;

public interface AccessTokenProvider {

    String generate(
        UUID usuarioId,
        UUID sessaoId,
        Instant issuedAt,
        Instant expiresAt
    );
}