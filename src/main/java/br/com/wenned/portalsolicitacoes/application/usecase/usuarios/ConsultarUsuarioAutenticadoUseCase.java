package br.com.wenned.portalsolicitacoes.application.usecase.usuarios;

import br.com.wenned.portalsolicitacoes.application.dto.autenticacao.AutenticacaoResultadoDTO.UsuarioAutenticado;
import br.com.wenned.portalsolicitacoes.application.port.out.usuarios.UsuarioRepository;
import br.com.wenned.portalsolicitacoes.domain.exception.autenticacao.AutenticacaoInvalidaException;

import java.util.UUID;

public class ConsultarUsuarioAutenticadoUseCase {

    private final UsuarioRepository usuarios;

    public ConsultarUsuarioAutenticadoUseCase(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public UsuarioAutenticado execute(UUID id) {
        var usuario = usuarios.findById(id)
            .orElseThrow(AutenticacaoInvalidaException::new);

        return new UsuarioAutenticado(
            usuario.getId(),
            usuario.getName(),
            usuario.getEmail()
        );
    }
}