package br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes;

import br.com.wenned.portalsolicitacoes.domain.exception.solicitacoes.OperacaoSolicitacaoInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolicitacaoOperacoesTest {

    private static final Instant CRIACAO =
        Instant.parse("2026-10-05T06:00:00Z");

    private static final Instant ATUALIZACAO = CRIACAO.plusSeconds(60);
    private static final Instant OPERACAO = CRIACAO.plusSeconds(120);

    @Test
    void deveEditarSolicitacaoAbertaPreservandoIdentidadeEProprietario() {
        var original = solicitacao(StatusSolicitacao.ABERTO);

        var editada = original.editar(
            "  Novo título  ",
            "  Nova descrição  ",
            CategoriaSolicitacao.RH,
            OPERACAO
        );

        assertThat(editada.getId()).isEqualTo(original.getId());
        assertThat(editada.getCodigo()).isEqualTo(original.getCodigo());
        assertThat(editada.getSolicitanteId())
            .isEqualTo(original.getSolicitanteId());
        assertThat(editada.getCreatedAt()).isEqualTo(CRIACAO);
        assertThat(editada.getUpdatedAt()).isEqualTo(OPERACAO);
        assertThat(editada.getVersion()).isEqualTo(original.getVersion());
        assertThat(editada.getStatus()).isEqualTo(StatusSolicitacao.ABERTO);
        assertThat(editada.getTitulo()).isEqualTo("Novo título");
        assertThat(editada.getDescricao()).isEqualTo("Nova descrição");
        assertThat(editada.getCategoria()).isEqualTo(CategoriaSolicitacao.RH);

        assertThat(original.getTitulo()).isEqualTo("Título original");
        assertThat(original.getUpdatedAt()).isEqualTo(ATUALIZACAO);
    }

    @ParameterizedTest
    @EnumSource(
        value = StatusSolicitacao.class,
        names = {"EM_ATENDIMENTO", "CONCLUIDO"}
    )
    void deveRecusarEdicaoDeSolicitacaoNaoAberta(StatusSolicitacao status) {
        var solicitacao = solicitacao(status);

        assertThatThrownBy(() -> solicitacao.editar(
            "Novo título",
            "Nova descrição",
            CategoriaSolicitacao.TI,
            OPERACAO
        )).isInstanceOf(OperacaoSolicitacaoInvalidaException.class);
    }

    @Test
    void devePermitirExclusaoDeSolicitacaoAberta() {
        var solicitacao = solicitacao(StatusSolicitacao.ABERTO);

        assertThatCode(solicitacao::validarExclusao)
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(
        value = StatusSolicitacao.class,
        names = {"EM_ATENDIMENTO", "CONCLUIDO"}
    )
    void deveRecusarExclusaoDeSolicitacaoNaoAberta(StatusSolicitacao status) {
        var solicitacao = solicitacao(status);

        assertThatThrownBy(solicitacao::validarExclusao)
            .isInstanceOf(OperacaoSolicitacaoInvalidaException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "ABERTO, EM_ATENDIMENTO",
        "EM_ATENDIMENTO, CONCLUIDO"
    })
    void devePermitirSomenteAvancoParaOProximoStatus(
        StatusSolicitacao atual,
        StatusSolicitacao destino
    ) {
        var original = solicitacao(atual);

        var alterada = original.alterarStatus(destino, OPERACAO);

        assertThat(alterada.getStatus()).isEqualTo(destino);
        assertThat(alterada.getUpdatedAt()).isEqualTo(OPERACAO);
        assertThat(alterada.getCreatedAt()).isEqualTo(CRIACAO);
        assertThat(alterada.getId()).isEqualTo(original.getId());
        assertThat(alterada.getCodigo()).isEqualTo(original.getCodigo());
        assertThat(alterada.getSolicitanteId())
            .isEqualTo(original.getSolicitanteId());
        assertThat(alterada.getTitulo()).isEqualTo(original.getTitulo());
        assertThat(alterada.getDescricao()).isEqualTo(original.getDescricao());
        assertThat(alterada.getCategoria()).isEqualTo(original.getCategoria());
        assertThat(alterada.getVersion()).isEqualTo(original.getVersion());

        assertThat(original.getStatus()).isEqualTo(atual);
    }

    @ParameterizedTest
    @CsvSource({
        "ABERTO, ABERTO",
        "ABERTO, CONCLUIDO",
        "EM_ATENDIMENTO, ABERTO",
        "EM_ATENDIMENTO, EM_ATENDIMENTO",
        "CONCLUIDO, ABERTO",
        "CONCLUIDO, EM_ATENDIMENTO",
        "CONCLUIDO, CONCLUIDO"
    })
    void deveRecusarOutrasTransicoes(
        StatusSolicitacao atual,
        StatusSolicitacao destino
    ) {
        var solicitacao = solicitacao(atual);

        assertThatThrownBy(() ->
            solicitacao.alterarStatus(destino, OPERACAO)
        ).isInstanceOf(OperacaoSolicitacaoInvalidaException.class);
    }

    @Test
    void deveRecusarDadosInvalidosNaEdicao() {
        var solicitacao = solicitacao(StatusSolicitacao.ABERTO);

        assertThatIllegalArgumentException().isThrownBy(() ->
            solicitacao.editar(
                " ",
                "Descrição válida",
                CategoriaSolicitacao.TI,
                OPERACAO
            )
        );
    }

    @Test
    void deveRecusarOperacoesComDataAnteriorAUltimaAtualizacao() {
        var solicitacao = solicitacao(StatusSolicitacao.ABERTO);
        Instant dataAnterior = ATUALIZACAO.minusSeconds(1);

        assertThatIllegalArgumentException().isThrownBy(() ->
            solicitacao.editar(
                "Novo título",
                "Nova descrição",
                CategoriaSolicitacao.TI,
                dataAnterior
            )
        );

        assertThatIllegalArgumentException().isThrownBy(() ->
            solicitacao.alterarStatus(
                StatusSolicitacao.EM_ATENDIMENTO,
                dataAnterior
            )
        );
    }

    @Test
    void deveRecusarNovoStatusAusente() {
        var solicitacao = solicitacao(StatusSolicitacao.ABERTO);

        assertThatIllegalArgumentException().isThrownBy(() ->
            solicitacao.alterarStatus(null, OPERACAO)
        );
    }

    private Solicitacao solicitacao(StatusSolicitacao status) {
        return Solicitacao.reconstituir(
            UUID.randomUUID(),
            42L,
            "Título original",
            "Descrição original",
            CategoriaSolicitacao.TI,
            status,
            UUID.randomUUID(),
            CRIACAO,
            ATUALIZACAO,
            3
        );
    }
}