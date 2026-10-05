package br.com.wenned.portalsolicitacoes.application.usecase.dashboard;

import br.com.wenned.portalsolicitacoes.application.dto.dashboard.DashboardResultadoDTO;
import br.com.wenned.portalsolicitacoes.application.port.out.dashboard.DashboardRepository;

import java.util.Objects;
import java.util.UUID;

public final class ConsultarDashboardUseCase {

    private final DashboardRepository repository;

    public ConsultarDashboardUseCase(DashboardRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public DashboardResultadoDTO executar(UUID solicitanteId) {
        Objects.requireNonNull(
            solicitanteId,
            "Solicitante obrigatório."
        );

        return repository.consultarPorProprietario(solicitanteId);
    }
}