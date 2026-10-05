package br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SolicitacaoTest {

    private static final UUID SOLICITANTE_ID = UUID.randomUUID();
    private static final Instant AGORA =
        Instant.parse("2026-10-03T12:00:00Z");

    @Test
    void deveCriarSolicitacaoAbertaComProprietarioEDatasIniciais() {
        var solicitacao = criar(
            "  Acesso ao sistema  ",
            "  Preciso de acesso ao sistema interno.  "
        );

        assertThat(solicitacao.getId()).isNotNull();
        assertThat(solicitacao.getCodigo()).isNull();
        assertThat(solicitacao.getTitulo()).isEqualTo("Acesso ao sistema");
        assertThat(solicitacao.getDescricao())
            .isEqualTo("Preciso de acesso ao sistema interno.");
        assertThat(solicitacao.getCategoria())
            .isEqualTo(CategoriaSolicitacao.TI);
        assertThat(solicitacao.getStatus())
            .isEqualTo(StatusSolicitacao.ABERTO);
        assertThat(solicitacao.getSolicitanteId())
            .isEqualTo(SOLICITANTE_ID);
        assertThat(solicitacao.getCreatedAt()).isEqualTo(AGORA);
        assertThat(solicitacao.getUpdatedAt()).isEqualTo(AGORA);
        assertThat(solicitacao.getVersion()).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void deveRecusarTituloAusenteOuEmBranco(String titulo) {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> criar(titulo, "Descrição válida"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void deveRecusarDescricaoAusenteOuEmBranco(String descricao) {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> criar("Título válido", descricao));
    }

    @Test
    void deveAceitarLimitesConsiderandoPontosDeCodigoUnicode() {
        String titulo = "😀".repeat(150);
        String descricao = "😀".repeat(5000);

        var solicitacao = criar(titulo, descricao);

        assertThat(solicitacao.getTitulo()).isEqualTo(titulo);
        assertThat(solicitacao.getDescricao()).isEqualTo(descricao);
    }

    @Test
    void deveRecusarTituloAcimaDoLimite() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> criar(
                "a".repeat(151),
                "Descrição válida"
            ));
    }

    @Test
    void deveRecusarDescricaoAcimaDoLimite() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> criar(
                "Título válido",
                "a".repeat(5001)
            ));
    }

    @Test
    void deveExigirCategoriaSolicitanteEData() {
        assertThatNullPointerException().isThrownBy(() ->
            Solicitacao.criar(
                "Título",
                "Descrição",
                null,
                SOLICITANTE_ID,
                AGORA
            )
        );

        assertThatNullPointerException().isThrownBy(() ->
            Solicitacao.criar(
                "Título",
                "Descrição",
                CategoriaSolicitacao.TI,
                null,
                AGORA
            )
        );

        assertThatNullPointerException().isThrownBy(() ->
            Solicitacao.criar(
                "Título",
                "Descrição",
                CategoriaSolicitacao.TI,
                SOLICITANTE_ID,
                null
            )
        );
    }

    @Test
    void deveReconstituirPreservandoIdentidadeEEstadoPersistido() {
        UUID id = UUID.randomUUID();
        Instant atualizacao = AGORA.plusSeconds(60);

        var solicitacao = Solicitacao.reconstituir(
            id,
            42L,
            "Título",
            "Descrição",
            CategoriaSolicitacao.RH,
            StatusSolicitacao.EM_ATENDIMENTO,
            SOLICITANTE_ID,
            AGORA,
            atualizacao,
            2
        );

        assertThat(solicitacao.getId()).isEqualTo(id);
        assertThat(solicitacao.getCodigo()).isEqualTo(42L);
        assertThat(solicitacao.getCategoria())
            .isEqualTo(CategoriaSolicitacao.RH);
        assertThat(solicitacao.getStatus())
            .isEqualTo(StatusSolicitacao.EM_ATENDIMENTO);
        assertThat(solicitacao.getCreatedAt()).isEqualTo(AGORA);
        assertThat(solicitacao.getUpdatedAt()).isEqualTo(atualizacao);
        assertThat(solicitacao.getVersion()).isEqualTo(2);
    }

    @Test
    void deveRecusarEstadoPersistidoInconsistente() {
        assertThatNullPointerException().isThrownBy(() ->
            reconstituir(null, AGORA, 0)
        );

        assertThatIllegalArgumentException().isThrownBy(() ->
            reconstituir(0L, AGORA, 0)
        );

        assertThatIllegalArgumentException().isThrownBy(() ->
            reconstituir(1L, AGORA.minusSeconds(1), 0)
        );

        assertThatIllegalArgumentException().isThrownBy(() ->
            reconstituir(1L, AGORA, -1)
        );
    }

    @Test
    void deveDefinirIgualdadePeloIdentificador() {
        var original = criar("Título", "Descrição");

        var reconstituida = Solicitacao.reconstituir(
            original.getId(),
            1L,
            original.getTitulo(),
            original.getDescricao(),
            original.getCategoria(),
            original.getStatus(),
            original.getSolicitanteId(),
            original.getCreatedAt(),
            original.getUpdatedAt(),
            original.getVersion()
        );

        assertThat(original).isEqualTo(reconstituida);
        assertThat(original.hashCode()).isEqualTo(reconstituida.hashCode());
        assertThat(original).isNotEqualTo(criar("Título", "Descrição"));
    }

    private Solicitacao criar(String titulo, String descricao) {
        return Solicitacao.criar(
            titulo,
            descricao,
            CategoriaSolicitacao.TI,
            SOLICITANTE_ID,
            AGORA
        );
    }

    private Solicitacao reconstituir(
        Long codigo,
        Instant updatedAt,
        long version
    ) {
        return Solicitacao.reconstituir(
            UUID.randomUUID(),
            codigo,
            "Título",
            "Descrição",
            CategoriaSolicitacao.TI,
            StatusSolicitacao.ABERTO,
            SOLICITANTE_ID,
            AGORA,
            updatedAt,
            version
        );
    }
}