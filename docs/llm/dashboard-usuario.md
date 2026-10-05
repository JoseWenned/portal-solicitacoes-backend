# Registro de apoio de LLM — dashboard por usuário

## Objetivo

Apoiar a implementação dos indicadores de solicitações
com isolamento por proprietário.

## Apoio recebido

- Proposta do resultado e da porta de consulta.
- Proposta do caso de uso.
- Consulta agregada com JdbcTemplate.
- Composição das dependências pelo Spring.
- Controller e DTO de resposta.
- Documentação inicial da funcionalidade.

## Decisões adotadas

- Preservar a organização por camadas.
- Manter aplicação independente de frameworks.
- Obter o proprietário exclusivamente do JWT.
- Calcular os indicadores em uma única consulta.
- Retornar zero quando o usuário não possui solicitações.
- Considerar todas as solicitações do usuário.
- Evitar armazenamento da resposta em cache HTTP.

## Justificativa da consulta JDBC

A operação retorna somente agregados.
JdbcTemplate permite expressar a consulta diretamente,
sem carregar entidades ou alterar a persistência JPA existente.

A implementação fica restrita à infraestrutura.

## Validação

Execução desta implementação ainda não informada pelo desenvolvedor.

## Evidências informadas pelo desenvolvedor

Execução no Ubuntu/WSL:

- Quatro testes HTTP do dashboard aprovados.
- Suíte completa com ./mvnw verify: 145 testes.
- Nenhuma falha, erro ou teste ignorado.
- BUILD SUCCESS.

Foram verificados contagens, isolamento, resultado vazio,
autenticação e atualização dos indicadores após operações reais.

Os resultados foram apresentados pelo desenvolvedor.
Não representam execução independente pela LLM.

## Pendências

- Execução da CI desta branch.
- OpenAPI e revisão final do backend em etapas posteriores.