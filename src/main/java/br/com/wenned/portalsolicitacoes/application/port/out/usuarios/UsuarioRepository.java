package br.com.wenned.portalsolicitacoes.application.port.out.usuarios;

import java.util.Optional;
import java.util.UUID;

import br.com.wenned.portalsolicitacoes.domain.entity.usuarios.Usuario;

public interface UsuarioRepository {

    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findById(UUID id);

    Usuario save(Usuario usuario);
}