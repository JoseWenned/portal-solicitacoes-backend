package br.com.wenned.portalsolicitacoes.infrastructure.persistence.repository.solicitacoes;

import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.CategoriaSolicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.StatusSolicitacao;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.model.solicitacoes.SolicitacaoModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoRepositorioJPA
    extends JpaRepository<SolicitacaoModel, UUID>, JpaSpecificationExecutor<SolicitacaoModel> {

    Optional<SolicitacaoModel> findByIdAndSolicitanteId(
        UUID id,
        UUID solicitanteId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE SolicitacaoModel s
        SET s.titulo = :titulo,
            s.descricao = :descricao,
            s.categoria = :categoria,
            s.updatedAt = :updatedAt,
            s.version = s.version + 1
        WHERE s.id = :id
          AND s.solicitanteId = :solicitanteId
          AND s.version = :versaoEsperada
          AND s.status = :statusAberto
        """)
    int editarCondicionalmente(
        @Param("id") UUID id,
        @Param("solicitanteId") UUID solicitanteId,
        @Param("versaoEsperada") long versaoEsperada,
        @Param("statusAberto") StatusSolicitacao statusAberto,
        @Param("titulo") String titulo,
        @Param("descricao") String descricao,
        @Param("categoria") CategoriaSolicitacao categoria,
        @Param("updatedAt") Instant updatedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE SolicitacaoModel s
        SET s.status = :novoStatus,
            s.updatedAt = :updatedAt,
            s.version = s.version + 1
        WHERE s.id = :id
          AND s.solicitanteId = :solicitanteId
          AND s.version = :versaoEsperada
          AND s.status = :statusAnterior
        """)
    int alterarStatusCondicionalmente(
        @Param("id") UUID id,
        @Param("solicitanteId") UUID solicitanteId,
        @Param("versaoEsperada") long versaoEsperada,
        @Param("statusAnterior") StatusSolicitacao statusAnterior,
        @Param("novoStatus") StatusSolicitacao novoStatus,
        @Param("updatedAt") Instant updatedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        DELETE FROM SolicitacaoModel s
        WHERE s.id = :id
          AND s.solicitanteId = :solicitanteId
          AND s.version = :versaoEsperada
          AND s.status = :statusAberto
        """)
    int excluirCondicionalmente(
        @Param("id") UUID id,
        @Param("solicitanteId") UUID solicitanteId,
        @Param("versaoEsperada") long versaoEsperada,
        @Param("statusAberto") StatusSolicitacao statusAberto
    );
}