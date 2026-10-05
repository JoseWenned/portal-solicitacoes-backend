# Dashboard por usuário

## Objetivo

Disponibilizar indicadores das solicitações do usuário autenticado.

## Contrato HTTP

GET /api/v1/dashboard

Exige Bearer JWT.

Resposta HTTP 200:

{
  "total": 0,
  "abertas": 0,
  "emAtendimento": 0,
  "concluidas": 0
}

Os valores representam todas as solicitações do proprietário,
independentemente dos filtros utilizados na listagem.

Usuário sem solicitações recebe todos os valores iguais a zero.
Autenticação ausente ou inválida retorna HTTP 401.

A resposta utiliza Cache-Control: no-store.

## Aplicação

ConsultarDashboardUseCase recebe o identificador do usuário autenticado
e consulta a porta DashboardRepository.

DashboardResultado representa as contagens.

A aplicação não depende de Spring, JPA ou JDBC.

## Infraestrutura

DashboardRepositoryAdapter utiliza JdbcTemplate para executar
uma única consulta de agregação no PostgreSQL.

O proprietário é uma condição obrigatória da consulta.
Seu identificador é enviado como parâmetro SQL.

A consulta calcula o total e as contagens por status.
Nenhuma lista de solicitações é carregada em memória.

A consulta JDBC convive com os adaptadores JPA existentes.
Nenhuma migration foi alterada.

## Apresentação

DashboardController obtém o proprietário do sub do JWT validado.
Não recebe identificador de usuário por query ou corpo.

DashboardResponseDTO define o contrato público.

## Validação confirmada

Resultados informados pelo desenvolvedor no Ubuntu/WSL:

- Quatro testes HTTP do dashboard aprovados.
- Contagens e isolamento por proprietário verificados.
- Usuário sem solicitações recebe valores zero.
- Indicadores atualizados após transições de status e exclusão.
- Total correspondente à soma das contagens por status.
- Cache-Control: no-store verificado.
- Suíte completa com ./mvnw verify: 145 testes.
- Nenhuma falha, erro ou teste ignorado.
- BUILD SUCCESS.

CI desta branch: pendente.