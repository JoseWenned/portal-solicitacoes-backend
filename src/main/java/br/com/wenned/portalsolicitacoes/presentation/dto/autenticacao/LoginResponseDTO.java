package br.com.wenned.portalsolicitacoes.presentation.dto.autenticacao;

import br.com.wenned.portalsolicitacoes.application.dto.autenticacao.AutenticacaoResultadoDTO;

public record LoginResponseDTO(
    String accessToken,
    String tokenType,
    long expiresIn,
    AutenticacaoResultadoDTO.UsuarioAutenticado user
) {

    public static LoginResponseDTO from(AutenticacaoResultadoDTO resultado) {
        return new LoginResponseDTO(
            resultado.accessToken(),
            "Bearer",
            resultado.expiresIn(),
            resultado.usuario()
        );
    }

    @Override
    public String toString() {
        return "LoginResponse[token omitido]";
    }
}