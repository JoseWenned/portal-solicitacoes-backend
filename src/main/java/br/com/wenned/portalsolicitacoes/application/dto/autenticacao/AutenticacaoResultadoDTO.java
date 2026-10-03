package br.com.wenned.portalsolicitacoes.application.dto.autenticacao;

import java.time.Instant;
import java.util.UUID;

public record AutenticacaoResultadoDTO(
    String accessToken,
    long expiresIn,
    String refreshToken,
    Instant sessionExpiresAt,
    UsuarioAutenticado usuario
) {

    public record UsuarioAutenticado(
        UUID id,
        String name,
        String email
    ) {
    }

    @Override
    public String toString() {
        return "AutenticacaoResultado[credenciais omitidas]";
    }
}