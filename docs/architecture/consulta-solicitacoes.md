# Consulta de solicitações

## Objetivo

Permitir consulta individual e listagem paginada das solicitações
do usuário autenticado, com filtros por status e categoria.

## Organização

O domínio e a aplicação permanecem independentes de Spring e JPA.

- ConsultarSolicitacaoUseCase coordena a consulta individual.
- ListarSolicitacoesUseCase valida a paginação e coordena a listagem.
- SolicitacaoRepository define os contratos de persistência.
- SolicitacaoRepositoryAdapter implementa as consultas.
- SolicitacaoRepositorioJPA utiliza Spring Data JPA.
- SolicitacaoController disponibiliza os endpoints HTTP.

## Consulta individual

GET /api/v1/solicitacoes/{id}

O identificador do proprietário é obtido do JWT validado.

A consulta combina ID da solicitação e proprietário
na mesma condição de busca.

Solicitações inexistentes ou pertencentes a outro usuário
retornam HTTP 404, com o mesmo código e mensagem de erro.

Não é realizada uma consulta global para informar se o registro
pertence a outra pessoa.

Respostas:

- 200: solicitação encontrada para o usuário autenticado.
- 400: identificador com formato inválido.
- 401: autenticação ausente ou inválida.
- 404: solicitação não encontrada no conjunto do usuário.

## Listagem paginada

GET /api/v1/solicitacoes

Parâmetros:

- page: índice iniciado em zero; padrão 0.
- size: de 1 a 100; padrão 20.
- status: opcional.
- categoria: opcional.

Os filtros são combinados com AND.
A condição de proprietário é obrigatória em todas as buscas.

A ordenação é createdAt DESC, codigo DESC.
O código funciona como desempate para datas iguais.

O adaptador utiliza Specification para compor os filtros
e PageRequest para paginação.

PaginaResultado mantém o contrato da aplicação independente
de Page e Pageable do Spring Data.

## Respostas públicas

A consulta individual retorna id, codigo, titulo, descricao,
categoria, status, solicitanteId, createdAt e updatedAt.

A listagem retorna:

- content: solicitações da página.
- page: índice da página.
- size: tamanho solicitado.
- totalElements: quantidade correspondente aos filtros.
- totalPages: quantidade de páginas.

Conteúdo e totais são limitados ao proprietário e aos filtros.

A versão de persistência não é exposta.

Página além do intervalo retorna 200 com conteúdo vazio
e totais preservados.

Usuário sem solicitações recebe conteúdo vazio e totais zero.

Paginação inválida ou enum desconhecido retorna 400.
Ausência de autenticação retorna 401.

## Testes da consulta individual

Cinco testes HTTP verificam:

- Consulta da própria solicitação.
- Rejeição de acesso à solicitação de outro usuário.
- Solicitação inexistente.
- UUID inválido.
- Ausência de autenticação.

Cadastro, login e criação são realizados por HTTP.

## Testes da listagem

Onze execuções verificam:

- Isolamento do conteúdo e dos totais por proprietário.
- Paginação com múltiplas páginas.
- Página além do intervalo.
- Ordenação por data e desempate pelo código.
- Filtros individuais e combinados.
- Resultado sem correspondência.
- Usuário sem solicitações.
- Parâmetros inválidos.
- Ausência de autenticação.

As solicitações são preparadas diretamente no PostgreSQL de testes
para controlar datas, categorias e status.

Essa preparação não valida transições de status.

Os testes utilizam servidor HTTP em porta aleatória e PostgreSQL
temporário via Testcontainers, separado do banco do Compose.

## Validação confirmada

Resultados informados pelo desenvolvedor no Ubuntu/WSL:

- Consulta individual: cinco testes HTTP aprovados.
- Listagem: onze execuções HTTP aprovadas.
- Suíte completa com ./mvnw verify: 106 testes.
- Nenhuma falha, erro ou teste ignorado.
- BUILD SUCCESS.

CI desta branch: pendente.

## Limitações e próximas etapas

- A ordenação é fixa; não há parâmetro de ordenação personalizado.
- Não há busca textual nesta implementação.
- Edição, exclusão, alteração de status e dashboard serão
  implementados em etapas próprias.

## Filtros por título e período

A listagem aceita os parâmetros opcionais titulo, dataInicial
e dataFinal, combinados com os filtros existentes por AND.

O título utiliza busca parcial sem diferenciação entre maiúsculas
e minúsculas. Espaços externos são removidos; texto vazio não aplica
filtro. O limite é de 150 pontos de código Unicode.
Os caracteres % e _ são tratados literalmente.

O período considera createdAt e datas no formato YYYY-MM-DD,
interpretadas no fuso America/Sao_Paulo.

A data inicial é inclusiva. A data final inclui todo o dia informado,
utilizando o início do dia seguinte como limite exclusivo.

É possível informar somente uma das datas.
Intervalo invertido ou data malformada retorna HTTP 400.

Todas as buscas e seus totais permanecem limitados ao proprietário.

### Validação

- Testes HTTP da listagem: 18 execuções aprovadas.
- Busca parcial por título, normalização de espaços e comparação
  sem diferenciação de maiúsculas e minúsculas verificadas.
- Caracteres % e _ tratados como texto literal.
- Limites inclusivos do período no fuso America/Sao_Paulo verificados.
- Consulta com apenas uma das datas verificada.
- Combinação de todos os filtros e isolamento por proprietário verificados.
- Intervalo invertido e datas malformadas retornam HTTP 400.
- Suíte completa: 153 testes, sem falhas, erros ou testes ignorados.
- ./mvnw verify: BUILD SUCCESS.
- CI desta branch: pendente.