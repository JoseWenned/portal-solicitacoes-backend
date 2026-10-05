package br.com.wenned.portalsolicitacoes.application.dto.solicitacoes;

import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.StatusSolicitacao;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusSolicitacaoRequestDTO(

    @NotNull(message = "Status obrigatório.")
    StatusSolicitacao status

) {
}