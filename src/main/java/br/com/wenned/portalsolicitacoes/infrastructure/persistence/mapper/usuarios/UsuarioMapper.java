package br.com.wenned.portalsolicitacoes.infrastructure.persistence.mapper.usuarios;

import br.com.wenned.portalsolicitacoes.domain.entity.usuarios.Usuario;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.model.usuarios.UsuarioModel;

public final class UsuarioMapper {

    private UsuarioMapper() {}

    public static UsuarioModel toModel(Usuario usuario) {
        return new UsuarioModel(
            usuario.getId(),
            usuario.getName(),
            usuario.getEmail(),
            usuario.getPasswordHash(),
            usuario.getCreatedAt()
        );
    }

    public static Usuario toDomain(UsuarioModel model) {
        return Usuario.reconstituir(
            model.getId(),
            model.getName(),
            model.getEmail(),
            model.getPasswordHash(),
            model.getCreatedAt()
        );
    }
}