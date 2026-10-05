package br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.dto.result.PaginaResultadoDTO;
import br.com.wenned.portalsolicitacoes.application.dto.solicitacoes.ConsultarSolicitacaoResultadoDTO;
import br.com.wenned.portalsolicitacoes.application.port.out.solicitacoes.SolicitacaoRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.CategoriaSolicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.StatusSolicitacao;

import java.util.Objects;
import java.util.UUID;

public final class ListarSolicitacoesUseCase {

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

        var pagina = repository.listarPorProprietario(
            solicitanteId,
            status,
            categoria,
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
}