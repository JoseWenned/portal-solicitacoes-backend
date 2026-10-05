package br.com.wenned.portalsolicitacoes.application.dto.result;

import java.util.List;

public record PaginaResultadoDTO<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public PaginaResultadoDTO {
        content = List.copyOf(content);
    }
}