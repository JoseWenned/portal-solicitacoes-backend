package br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes;

import br.com.wenned.portalsolicitacoes.domain.exception.solicitacoes.OperacaoSolicitacaoInvalidaException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Solicitacao {

    private final UUID id;
    private final Long codigo;
    private final String titulo;
    private final String descricao;
    private final CategoriaSolicitacao categoria;
    private final StatusSolicitacao status;
    private final UUID solicitanteId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final long version;

    private Solicitacao(
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
        this.id = Objects.requireNonNull(id, "Identificador obrigatório.");

        if (codigo != null && codigo <= 0) {
            throw new IllegalArgumentException("Código deve ser positivo.");
        }

        this.codigo = codigo;
        this.titulo = validarTexto(titulo, "Título", 150);
        this.descricao = validarTexto(descricao, "Descrição", 5000);
        this.categoria = Objects.requireNonNull(
            categoria,
            "Categoria obrigatória."
        );
        this.status = Objects.requireNonNull(status, "Status obrigatório.");
        this.solicitanteId = Objects.requireNonNull(
            solicitanteId,
            "Solicitante obrigatório."
        );
        this.createdAt = Objects.requireNonNull(
            createdAt,
            "Data de criação obrigatória."
        );
        this.updatedAt = Objects.requireNonNull(
            updatedAt,
            "Data de atualização obrigatória."
        );

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                "Data de atualização não pode preceder a criação."
            );
        }

        if (version < 0) {
            throw new IllegalArgumentException(
                "Versão não pode ser negativa."
            );
        }

        this.version = version;
    }

    public static Solicitacao criar(
        String titulo,
        String descricao,
        CategoriaSolicitacao categoria,
        UUID solicitanteId,
        Instant agora
    ) {
        return new Solicitacao(
            UUID.randomUUID(),
            null,
            titulo,
            descricao,
            categoria,
            StatusSolicitacao.ABERTO,
            solicitanteId,
            agora,
            agora,
            0
        );
    }

    public static Solicitacao reconstituir(
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
        Objects.requireNonNull(codigo, "Código persistido obrigatório.");

        return new Solicitacao(
            id,
            codigo,
            titulo,
            descricao,
            categoria,
            status,
            solicitanteId,
            createdAt,
            updatedAt,
            version
        );
    }

    public Solicitacao editar(
        String novoTitulo,
        String novaDescricao,
        CategoriaSolicitacao novaCategoria,
        Instant agora
    ) {
        exigirAberta("editar");
        validarDataOperacao(agora);

        return new Solicitacao(
            id,
            codigo,
            novoTitulo,
            novaDescricao,
            novaCategoria,
            status,
            solicitanteId,
            createdAt,
            agora,
            version
        );
    }

    public void validarExclusao() {
        exigirAberta("excluir");
    }

    public Solicitacao alterarStatus(
        StatusSolicitacao novoStatus,
        Instant agora
    ) {
        if (novoStatus == null) {
            throw new IllegalArgumentException("Novo status obrigatório.");
        }

        boolean transicaoPermitida =
            (status == StatusSolicitacao.ABERTO
                && novoStatus == StatusSolicitacao.EM_ATENDIMENTO)
            || (status == StatusSolicitacao.EM_ATENDIMENTO
                && novoStatus == StatusSolicitacao.CONCLUIDO);

        if (!transicaoPermitida) {
            throw new OperacaoSolicitacaoInvalidaException(
                "Transição de status não permitida."
            );
        }

        validarDataOperacao(agora);

        return new Solicitacao(
            id,
            codigo,
            titulo,
            descricao,
            categoria,
            novoStatus,
            solicitanteId,
            createdAt,
            agora,
            version
        );
    }

    private void exigirAberta(String operacao) {
        if (status != StatusSolicitacao.ABERTO) {
            throw new OperacaoSolicitacaoInvalidaException(
                "Somente solicitações abertas podem ser alteradas pela operação de "
                    + operacao + "."
            );
        }
    }

    private void validarDataOperacao(Instant agora) {
        Objects.requireNonNull(agora, "Data da operação obrigatória.");

        if (agora.isBefore(updatedAt)) {
            throw new IllegalArgumentException(
                "Data da operação não pode preceder a última atualização."
            );
        }
    }

    private static String validarTexto(
        String valor,
        String campo,
        int limite
    ) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " obrigatório.");
        }

        String normalizado = valor.strip();
        int tamanho = normalizado.codePointCount(
            0,
            normalizado.length()
        );

        if (tamanho == 0 || tamanho > limite) {
            throw new IllegalArgumentException(
                campo + " deve possuir entre 1 e " + limite + " caracteres."
            );
        }

        return normalizado;
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        return other instanceof Solicitacao solicitacao
            && id.equals(solicitacao.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Solicitacao[id=" + id
            + ", codigo=" + codigo
            + ", status=" + status + "]";
    }
}