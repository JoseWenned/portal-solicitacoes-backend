package br.com.wenned.portalsolicitacoes.infrastructure.persistence.mapper.solicitacoes;

import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.Solicitacao;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.model.solicitacoes.SolicitacaoModel;

import java.util.Objects;

public final class SolicitacaoMapper {

    private SolicitacaoMapper() {
    }

    public static SolicitacaoModel toModel(Solicitacao solicitacao) {
        Objects.requireNonNull(solicitacao);

        return new SolicitacaoModel(
            solicitacao.getId(),
            solicitacao.getCodigo(),
            solicitacao.getTitulo(),
            solicitacao.getDescricao(),
            solicitacao.getCategoria(),
            solicitacao.getStatus(),
            solicitacao.getSolicitanteId(),
            solicitacao.getCreatedAt(),
            solicitacao.getUpdatedAt(),
            solicitacao.getVersion()
        );
    }

    public static Solicitacao toDomain(SolicitacaoModel model) {
        Objects.requireNonNull(model);

        return Solicitacao.reconstituir(
            model.getId(),
            model.getCodigo(),
            model.getTitulo(),
            model.getDescricao(),
            model.getCategoria(),
            model.getStatus(),
            model.getSolicitanteId(),
            model.getCreatedAt(),
            model.getUpdatedAt(),
            model.getVersion()
        );
    }
}