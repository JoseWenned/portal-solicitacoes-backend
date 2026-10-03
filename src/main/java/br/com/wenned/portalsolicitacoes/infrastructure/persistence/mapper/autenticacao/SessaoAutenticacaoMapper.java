package br.com.wenned.portalsolicitacoes.infrastructure.persistence.mapper.autenticacao;

import br.com.wenned.portalsolicitacoes.domain.entity.autenticacao.SessaoAutenticacao;
import br.com.wenned.portalsolicitacoes.infrastructure.persistence.model.autenticacao.SessaoAutenticacaoModel;

public final class SessaoAutenticacaoMapper {

    private SessaoAutenticacaoMapper() {}

    public static SessaoAutenticacaoModel toModel(
        SessaoAutenticacao sessao
    ) {
        return new SessaoAutenticacaoModel(
            sessao.getId(),
            sessao.getUsuarioId(),
            sessao.getRefreshTokenHash(),
            sessao.getCreatedAt(),
            sessao.getExpiresAt(),
            sessao.getRevokedAt(),
            sessao.getVersion()
        );
    }

    public static SessaoAutenticacao toDomain(
        SessaoAutenticacaoModel model
    ) {
        return SessaoAutenticacao.reconstituir(
            model.getId(),
            model.getUsuarioId(),
            model.getRefreshTokenHash(),
            model.getCreatedAt(),
            model.getExpiresAt(),
            model.getRevokedAt(),
            model.getVersion()
        );
    }
}