package br.com.wenned.portalsolicitacoes.application.dto.dashboard;

public record DashboardResultadoDTO(
    long total,
    long abertas,
    long emAtendimento,
    long concluidas
) {
}