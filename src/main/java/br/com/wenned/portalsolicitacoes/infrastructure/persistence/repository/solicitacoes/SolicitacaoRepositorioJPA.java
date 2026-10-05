package br.com.wenned.portalsolicitacoes.infrastructure.persistence.repository.solicitacoes;

import br.com.wenned.portalsolicitacoes.infrastructure.persistence.model.solicitacoes.SolicitacaoModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoRepositorioJPA extends JpaRepository<SolicitacaoModel, UUID>, JpaSpecificationExecutor<SolicitacaoModel> {

    Optional<SolicitacaoModel> findByIdAndSolicitanteId(
        UUID id,
        UUID solicitanteId
    );
}