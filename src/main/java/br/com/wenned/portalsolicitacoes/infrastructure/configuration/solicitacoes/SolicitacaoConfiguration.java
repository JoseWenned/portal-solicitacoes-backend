package br.com.wenned.portalsolicitacoes.infrastructure.configuration.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.port.out.solicitacoes.SolicitacaoRepository;
import br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes.ConsultarSolicitacaoUseCase;
import br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes.CriarSolicitacaoUseCase;
import br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes.ListarSolicitacoesUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SolicitacaoConfiguration {

    @Bean
    public CriarSolicitacaoUseCase criarSolicitacaoUseCase(
        SolicitacaoRepository repository,
        Clock clock
    ) {
        return new CriarSolicitacaoUseCase(repository, clock);
    }

    @Bean
    public ConsultarSolicitacaoUseCase consultarSolicitacaoUseCase(
        SolicitacaoRepository repository
    ) {
        return new ConsultarSolicitacaoUseCase(repository);
    }

    @Bean
    public ListarSolicitacoesUseCase listarSolicitacoesUseCase(
        SolicitacaoRepository repository
    ) {
        return new ListarSolicitacoesUseCase(repository);
    }
}