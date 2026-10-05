package br.com.wenned.portalsolicitacoes.infrastructure.configuration;

import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.SessaoAutenticacaoRepository;
import br.com.wenned.portalsolicitacoes.application.usecase.autenticacao.EncerrarSessaoAutenticadaUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class EncerramentoSessaoConfiguration {

    @Bean
    public EncerrarSessaoAutenticadaUseCase encerrarSessaoAutenticadaUseCase(
        SessaoAutenticacaoRepository sessoes,
        Clock clock
    ) {
        return new EncerrarSessaoAutenticadaUseCase(sessoes, clock);
    }
}