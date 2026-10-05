package br.com.wenned.portalsolicitacoes.application.usecase.autenticacao;

import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.SessaoAutenticacaoRepository;
import br.com.wenned.portalsolicitacoes.domain.exception.autenticacao.AutenticacaoInvalidaException;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class EncerrarSessaoAutenticadaUseCase {

    private final SessaoAutenticacaoRepository sessoes;
    private final Clock clock;

    public EncerrarSessaoAutenticadaUseCase(
        SessaoAutenticacaoRepository sessoes,
        Clock clock
    ) {
        this.sessoes = Objects.requireNonNull(sessoes);
        this.clock = Objects.requireNonNull(clock);
    }

    public void executar(UUID usuarioId, UUID sessaoId) {
        Objects.requireNonNull(usuarioId);
        Objects.requireNonNull(sessaoId);

        var sessao = sessoes.findById(sessaoId)
            .filter(encontrada ->
                encontrada.getUsuarioId().equals(usuarioId)
            )
            .orElseThrow(AutenticacaoInvalidaException::new);

        sessoes.revoke(sessao.getId(), clock.instant());
    }
}