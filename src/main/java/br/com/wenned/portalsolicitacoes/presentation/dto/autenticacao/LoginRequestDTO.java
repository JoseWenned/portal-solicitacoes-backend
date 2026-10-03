package br.com.wenned.portalsolicitacoes.presentation.dto.autenticacao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "O e-mail é inválido.")
    @Size(max = 254)
    String email,

    @NotBlank(message = "A senha é obrigatória.")
    @Size(max = 72)
    String password
) {

    @Override
    public String toString() {
        return "LoginRequest[conteudo omitido]";
    }
}