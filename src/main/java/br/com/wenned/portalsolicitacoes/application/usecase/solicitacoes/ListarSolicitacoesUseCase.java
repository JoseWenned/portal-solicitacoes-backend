package br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.dto.result.PaginaResultadoDTO;
import br.com.wenned.portalsolicitacoes.application.dto.solicitacoes.ConsultarSolicitacaoResultadoDTO;
import br.com.wenned.portalsolicitacoes.application.port.out.solicitacoes.SolicitacaoRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.CategoriaSolicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.StatusSolicitacao;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

public final class ListarSolicitacoesUseCase {

    private static final ZoneId FUSO_CONSULTA =
        ZoneId.of("America/Sao_Paulo");

    private final SolicitacaoRepository repository;

    public ListarSolicitacoesUseCase(SolicitacaoRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public PaginaResultadoDTO<ConsultarSolicitacaoResultadoDTO> executar(
        UUID solicitanteId,
        StatusSolicitacao status,
        CategoriaSolicitacao categoria,
        int page,
        int size
    ) {
        return executar(
            solicitanteId, status, categoria,
            null, null, null, page, size
        );
    }

    public PaginaResultadoDTO<ConsultarSolicitacaoResultadoDTO> executar(
        UUID solicitanteId,
        StatusSolicitacao status,
        CategoriaSolicitacao categoria,
        String titulo,
        LocalDate dataInicial,
        LocalDate dataFinal,
        int page,
        int size
    ) {
        Objects.requireNonNull(solicitanteId, "Solicitante obrigatório.");

        if (page < 0) {
            throw new IllegalArgumentException(
                "A página não pode ser negativa."
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                "O tamanho da página deve estar entre 1 e 100."
            );
        }

        if (dataInicial != null && dataFinal != null
            && dataInicial.isAfter(dataFinal)) {
            throw new IllegalArgumentException(
                "A data inicial não pode ser posterior à data final."
            );
        }

        String tituloNormalizado = normalizarTitulo(titulo);
        Instant inicioInclusivo;
        Instant fimExclusivo;

        try {
            inicioInclusivo = dataInicial == null
                ? null
                : dataInicial.atStartOfDay(FUSO_CONSULTA).toInstant();

            fimExclusivo = dataFinal == null
                ? null
                : dataFinal.plusDays(1)
                    .atStartOfDay(FUSO_CONSULTA)
                    .toInstant();
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(
                "O período informado está fora do intervalo permitido."
            );
        }

        var pagina = repository.listarPorProprietario(
            solicitanteId,
            status,
            categoria,
            tituloNormalizado,
            inicioInclusivo,
            fimExclusivo,
            page,
            size
        );

        var content = pagina.content().stream()
            .map(solicitacao -> new ConsultarSolicitacaoResultadoDTO(
                solicitacao.getId(),
                solicitacao.getCodigo(),
                solicitacao.getTitulo(),
                solicitacao.getDescricao(),
                solicitacao.getCategoria(),
                solicitacao.getStatus(),
                solicitacao.getSolicitanteId(),
                solicitacao.getCreatedAt(),
                solicitacao.getUpdatedAt()
            ))
            .toList();

        return new PaginaResultadoDTO<>(
            content,
            pagina.page(),
            pagina.size(),
            pagina.totalElements(),
            pagina.totalPages()
        );
    }

    private static String normalizarTitulo(String titulo) {
        if (titulo == null) {
            return null;
        }

        String normalizado = titulo.strip();

        if (normalizado.isEmpty()) {
            return null;
        }

        if (normalizado.codePointCount(0, normalizado.length()) > 150) {
            throw new IllegalArgumentException(
                "O filtro de título deve possuir até 150 caracteres."
            );
        }

        return normalizado;
    }
}