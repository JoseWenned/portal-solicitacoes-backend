package br.com.wenned.portalsolicitacoes.presentation.dto.dashboard;

public record DashboardResponseDTO(
    long total,
    long abertas,
    long emAtendimento,
    long concluidas
) {
}