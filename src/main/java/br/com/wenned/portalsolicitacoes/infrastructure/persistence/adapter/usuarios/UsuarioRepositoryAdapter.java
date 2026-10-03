package br.com.wenned.portalsolicitacoes.infrastructure.persistence.adapter.usuarios;

import br.com.wenned.portalsolicitacoes.application.port.out.usuarios.UsuarioRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.usuarios.Usuario;
import br.com.wenned.portalsolicitacoes.domain.exception.usuarios.EmailJaCadastradoException;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.mapper.usuarios.UsuarioMapper;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.model.usuarios.UsuarioModel;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.repository.usuarios.UsuarioRepositoryJPA;

import java.util.Optional;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioRepositoryJPA repository;

    public UsuarioRepositoryAdapter(UsuarioRepositoryJPA repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        return repository.findByEmail(email)
            .map(UsuarioMapper::toDomain);
    }

    @Override
    public Optional<Usuario> findById(UUID id) {
        return repository.findById(id)
            .map(UsuarioMapper::toDomain);
    }

    @Override
    public Usuario save(Usuario usuario) {
        try {
            UsuarioModel saved = repository.saveAndFlush(
                UsuarioMapper.toModel(usuario)
            );

            return UsuarioMapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailUniqueViolation(exception)) {
                throw new EmailJaCadastradoException();
            }

            throw exception;
        }
    }

    private boolean isEmailUniqueViolation(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && "uk_usuarios_email".equals(
                        violation.getConstraintName()
                    )) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}