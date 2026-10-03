package br.com.wenned.portalsolicitacoes.application.port.out.usuarios;

import br.com.wenned.portalsolicitacoes.domain.entity.usuarios.Usuario;

public interface UsuarioRepository {

    boolean existsByEmail(String email);

    Usuario save(Usuario usuario);
}