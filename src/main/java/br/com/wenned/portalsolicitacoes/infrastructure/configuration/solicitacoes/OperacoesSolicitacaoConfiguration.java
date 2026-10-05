package br.com.wenned.portalsolicitacoes.infrastructure.configuration.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.port.out.solicitacoes.SolicitacaoRepository;
import br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes.AlterarStatusSolicitacaoUseCase;
import br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes.EditarSolicitacaoUseCase;
import br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes.ExcluirSolicitacaoUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class OperacoesSolicitacaoConfiguration {

    @Bean
    public EditarSolicitacaoUseCase editarSolicitacaoUseCase(
        SolicitacaoRepository repository,
        Clock clock
    ) {
        return new EditarSolicitacaoUseCase(repository, clock);
    }

    @Bean
    public ExcluirSolicitacaoUseCase excluirSolicitacaoUseCase(
        SolicitacaoRepository repository
    ) {
        return new ExcluirSolicitacaoUseCase(repository);
    }

    @Bean
    public AlterarStatusSolicitacaoUseCase alterarStatusSolicitacaoUseCase(
        SolicitacaoRepository repository,
        Clock clock
    ) {
        return new AlterarStatusSolicitacaoUseCase(repository, clock);
    }
}