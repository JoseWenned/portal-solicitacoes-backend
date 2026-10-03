package br.com.wenned.portalsolicitacoes.presentation.dto.usuarios;

import java.time.Instant;
import java.util.UUID;

public record UsuarioResponse(
    UUID id,
    String name,
    String email,
    Instant createdAt
) {}