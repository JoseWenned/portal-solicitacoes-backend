package br.com.wenned.portalsolicitacoes.infrastructure.persistence.model.solicitacoes;

import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.CategoriaSolicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.StatusSolicitacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "solicitacoes")
public class SolicitacaoModel {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, insertable = false, updatable = false)
    private Long codigo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 5000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaSolicitacao categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSolicitacao status;

    @Column(name = "solicitante_id", nullable = false, updatable = false)
    private UUID solicitanteId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected SolicitacaoModel() {
    }

    public SolicitacaoModel(
        UUID id,
        Long codigo,
        String titulo,
        String descricao,
        CategoriaSolicitacao categoria,
        StatusSolicitacao status,
        UUID solicitanteId,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) {
        this.id = id;
        this.codigo = codigo;
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.status = status;
        this.solicitanteId = solicitanteId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public Long getCodigo() {
        return codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public CategoriaSolicitacao getCategoria() {
        return categoria;
    }

    public StatusSolicitacao getStatus() {
        return status;
    }

    public UUID getSolicitanteId() {
        return solicitanteId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}