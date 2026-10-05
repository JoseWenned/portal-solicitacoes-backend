package br.com.wenned.portalsolicitacoes.application.port.out.dashboard;

import br.com.wenned.portalsolicitacoes.application.dto.dashboard.DashboardResultadoDTO;

import java.util.UUID;

public interface DashboardRepository {

    DashboardResultadoDTO consultarPorProprietario(UUID solicitanteId);
}