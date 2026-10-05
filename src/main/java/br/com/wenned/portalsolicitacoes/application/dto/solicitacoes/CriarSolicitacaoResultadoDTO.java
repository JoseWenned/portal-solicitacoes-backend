package br.com.wenned.portalsolicitacoes.application.dto.solicitacoes;

import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.CategoriaSolicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.StatusSolicitacao;

import java.time.Instant;
import java.util.UUID;

public record CriarSolicitacaoResultadoDTO(
    UUID id,
    Long codigo,
    String titulo,
    String descricao,
    CategoriaSolicitacao categoria,
    StatusSolicitacao status,
    UUID solicitanteId,
    Instant createdAt,
    Instant updatedAt
) {
}