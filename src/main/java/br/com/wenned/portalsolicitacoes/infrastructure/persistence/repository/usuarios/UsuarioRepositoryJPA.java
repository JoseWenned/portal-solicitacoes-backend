package br.com.wenned.portalsolicitacoes.infrastructure.persistence.repository.usuarios;

import br.com.wenned.portalsolicitacoes.infrastructure.persistence.model.usuarios.UsuarioModel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositoryJPA extends JpaRepository<UsuarioModel, UUID> {

    boolean existsByEmail(String email);

    Optional<UsuarioModel> findByEmail(String email);
}