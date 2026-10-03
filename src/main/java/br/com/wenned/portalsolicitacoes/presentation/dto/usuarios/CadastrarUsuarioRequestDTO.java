package br.com.wenned.portalsolicitacoes.presentation.dto.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CadastrarUsuarioRequestDTO(
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 200, message = "O nome informado é muito longo.")
    String name,

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "O e-mail é inválido.")
    @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
    String email,

    @NotBlank(message = "A senha é obrigatória.")
    @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres.")
    String password
) {

    public CadastrarUsuarioRequestDTO {
        name = name == null ? null : name.strip();
        email = email == null
            ? null
            : email.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "CadastrarUsuarioRequest[conteudo omitido]";
    }
}