package br.com.wenned.portalsolicitacoes.infrastructure.persistence.repository.solicitacoes;

import br.com.wenned.portalsolicitacoes.infrastructure.persistence.model.solicitacoes.SolicitacaoModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SolicitacaoRepositorioJPA extends JpaRepository<SolicitacaoModel, UUID> {
}