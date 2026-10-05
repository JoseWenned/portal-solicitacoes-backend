package br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.port.out.solicitacoes.SolicitacaoRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.CategoriaSolicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.Solicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.StatusSolicitacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CriarSolicitacaoUseCaseTest {

    private static final Instant AGORA =
        Instant.parse("2026-10-03T12:00:00Z");

    private SolicitacaoRepository repository;
    private CriarSolicitacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(SolicitacaoRepository.class);

        useCase = new CriarSolicitacaoUseCase(
            repository,
            Clock.fixed(AGORA, ZoneOffset.UTC)
        );
    }

    @Test
    void devePersistirSolicitacaoDoUsuarioERetornarCodigoGerado() {
        UUID solicitanteId = UUID.randomUUID();

        when(repository.criar(any(Solicitacao.class)))
            .thenAnswer(invocation -> {
                Solicitacao recebida = invocation.getArgument(0);

                assertThat(recebida.getCodigo()).isNull();
                assertThat(recebida.getSolicitanteId())
                    .isEqualTo(solicitanteId);
                assertThat(recebida.getStatus())
                    .isEqualTo(StatusSolicitacao.ABERTO);
                assertThat(recebida.getCreatedAt()).isEqualTo(AGORA);
                assertThat(recebida.getUpdatedAt()).isEqualTo(AGORA);
                assertThat(recebida.getVersion()).isZero();

                return Solicitacao.reconstituir(
                    recebida.getId(),
                    42L,
                    recebida.getTitulo(),
                    recebida.getDescricao(),
                    recebida.getCategoria(),
                    recebida.getStatus(),
                    recebida.getSolicitanteId(),
                    recebida.getCreatedAt(),
                    recebida.getUpdatedAt(),
                    recebida.getVersion()
                );
            });

        var resultado = useCase.executar(
            solicitanteId,
            "  Acesso ao sistema  ",
            "  Preciso de acesso ao sistema interno.  ",
            CategoriaSolicitacao.TI
        );

        assertThat(resultado.id()).isNotNull();
        assertThat(resultado.codigo()).isEqualTo(42L);
        assertThat(resultado.titulo()).isEqualTo("Acesso ao sistema");
        assertThat(resultado.descricao())
            .isEqualTo("Preciso de acesso ao sistema interno.");
        assertThat(resultado.categoria())
            .isEqualTo(CategoriaSolicitacao.TI);
        assertThat(resultado.status()).isEqualTo(StatusSolicitacao.ABERTO);
        assertThat(resultado.solicitanteId()).isEqualTo(solicitanteId);
        assertThat(resultado.createdAt()).isEqualTo(AGORA);
        assertThat(resultado.updatedAt()).isEqualTo(AGORA);

        verify(repository).criar(any(Solicitacao.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void naoDevePersistirQuandoOsDadosForemInvalidos() {
        assertThatIllegalArgumentException().isThrownBy(() ->
            useCase.executar(
                UUID.randomUUID(),
                " ",
                "Descrição válida",
                CategoriaSolicitacao.TI
            )
        );

        verifyNoInteractions(repository);
    }

    @Test
    void naoDevePersistirSemSolicitante() {
        assertThatNullPointerException().isThrownBy(() ->
            useCase.executar(
                null,
                "Título válido",
                "Descrição válida",
                CategoriaSolicitacao.TI
            )
        );

        verifyNoInteractions(repository);
    }
}