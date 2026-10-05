package br.com.wenned.portalsolicitacoes.application.dto.solicitacoes;

import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.CategoriaSolicitacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EditarSolicitacaoRequestDTO(

    @NotBlank(message = "Título obrigatório.")
    String titulo,

    @NotBlank(message = "Descrição obrigatória.")
    String descricao,

    @NotNull(message = "Categoria obrigatória.")
    CategoriaSolicitacao categoria

) {
}