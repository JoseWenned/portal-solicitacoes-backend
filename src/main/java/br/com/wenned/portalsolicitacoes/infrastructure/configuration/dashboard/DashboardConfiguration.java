package br.com.wenned.portalsolicitacoes.infrastructure.configuration.dashboard;

import br.com.wenned.portalsolicitacoes.application.port.out.dashboard.DashboardRepository;
import br.com.wenned.portalsolicitacoes.application.usecase.dashboard.ConsultarDashboardUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DashboardConfiguration {

    @Bean
    public ConsultarDashboardUseCase consultarDashboardUseCase(
        DashboardRepository repository
    ) {
        return new ConsultarDashboardUseCase(repository);
    }
}