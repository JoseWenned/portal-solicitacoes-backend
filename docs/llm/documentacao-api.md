# Registro de apoio de LLM — documentação da API

## Objetivo

Apoiar a integração de OpenAPI e Swagger UI.

## Apoio recebido

- Verificação da linha springdoc compatível com Spring Boot 4.
- Proposta da configuração OpenAPI.
- Esquemas de Bearer, CSRF e refresh cookie.
- Ajuste dos códigos de resposta documentados.
- Liberação dos caminhos GET da documentação.
- Orientações para execução manual pelo Swagger.

## Decisões adotadas

- Manter a segurança dos endpoints de negócio.
- Preservar a validação JWT e de sessão.
- Preservar a proteção CSRF.
- Não persistir a autorização na interface Swagger.
- Restringir a geração aos controllers e caminhos da API.

## Evidências

O desenvolvedor informou compilação bem-sucedida
após adicionar a dependência springdoc.

A configuração completa ainda aguarda execução e validação.

## Pendências

- Verificar o contrato gerado e a interface.
- Confirmar a proteção dos endpoints de negócio.
- Atualizar README e executar a CI.