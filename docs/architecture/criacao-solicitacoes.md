# Criação de solicitações

## Objetivo

Implementar a criação de solicitações vinculadas ao usuário autenticado.

## Domínio

Solicitacao possui identidade UUID e código numérico gerado pelo banco.

Na criação:

- O status é sempre ABERTO.
- O solicitante é obrigatório.
- As datas de criação e atualização são iguais.
- A versão inicial é zero.
- O código permanece ausente até a persistência.

Título e descrição são obrigatórios, com limites de 150 e 5.000
pontos de código Unicode, respectivamente. Espaços externos
são removidos.

As categorias correspondem às permitidas pela migration existente:
TI, RH, COMPRAS, FINANCEIRO e INFRAESTRUTURA.

A entidade não depende de Spring ou JPA.
Sua igualdade é definida pelo UUID.

## Aplicação

CriarSolicitacaoUseCase recebe o identificador do usuário autenticado,
título, descrição e categoria.

O domínio valida os dados e estabelece o status inicial ABERTO.
O caso de uso solicita a persistência e retorna os dados públicos
da solicitação, incluindo o código gerado pelo banco.

SolicitacaoRepository define uma operação específica de criação.
Sua implementação permanece pendente.

Clock é recebido como dependência para controlar as datas nos testes.

A versão de persistência não é incluída no resultado público.
A integração HTTP deverá obter o proprietário do JWT, sem recebê-lo
no corpo da requisição.

## Persistência

A implementação reutiliza a tabela solicitacoes e sua migration existente.

- SolicitacaoModel representa o registro persistido.
- SolicitacaoMapper converte entre domínio e modelo.
- SolicitacaoRepositorioJPA disponibiliza a base para futuras consultas.
- SolicitacaoRepositoryAdapter implementa a criação.
- SolicitacaoConfiguration conecta o caso de uso ao Spring.

O UUID é criado pelo domínio.
O código numérico é gerado exclusivamente pelo PostgreSQL.

A coluna codigo não participa de INSERT ou UPDATE produzidos pelo JPA.

A criação utiliza persist, flush e refresh na mesma transação.
O refresh obtém o código gerado antes da conversão para o domínio.

O modelo utiliza @Version para controle de versão.
A criação exige uma entidade sem código e com versão zero.

Nenhuma alteração da migration aplicada foi realizada.

A operação de criação no banco ainda aguarda teste de integração.

## Propriedade

O endpoint deverá obter o identificador do solicitante da autenticação.
O cliente não poderá escolher o proprietário da solicitação.

Essa integração ainda está pendente.

## Teste de integração da persistência

Teste com PostgreSQL temporário via Testcontainers.

A cobertura verifica:

- Inserção de duas solicitações.
- Preservação dos identificadores UUID.
- Recuperação dos códigos gerados pelo PostgreSQL.
- Códigos positivos e distintos.
- Persistência de título, descrição e categoria.
- Associação ao solicitante.
- Status inicial ABERTO.
- Preservação das datas e versão inicial zero.

O usuário é preparado diretamente no banco como fixture.
O teste não repete os fluxos de cadastro e autenticação.

Não há exigência de códigos consecutivos.

Execução do novo teste: pendente.

## Apresentação e contrato HTTP

POST /api/v1/solicitacoes

Endpoint protegido por autenticação Bearer JWT.

Entrada:

- titulo
- descricao
- categoria

O identificador do solicitante é obtido do sub do JWT validado.
O DTO de entrada não permite definir proprietário, status ou código.

Bean Validation verifica campos obrigatórios.
O domínio verifica os limites de título e descrição em pontos
de código Unicode, após remover espaços externos.

Categorias aceitas:
TI, RH, COMPRAS, FINANCEIRO e INFRAESTRUTUTURA.

Resposta de sucesso: HTTP 201.

O corpo contém id, codigo, titulo, descricao, categoria, status,
solicitanteId, createdAt e updatedAt.

A versão de persistência não é exposta.

A resposta ainda não inclui Location, pois a consulta individual
será implementada em etapa posterior.

A criação utiliza autenticação pelo header Authorization.
O refresh token não é utilizado como credencial desse endpoint.

## Validação confirmada

- Persistência: inserção e recuperação do código gerado validadas.
- Testes HTTP de criação: 10 execuções aprovadas.
- Criação autenticada retorna HTTP 201.
- Proprietário obtido do JWT e status inicial ABERTO.
- Campos enviados pelo cliente não controlam proprietário ou status.
- Entradas inválidas são rejeitadas sem persistência.
- Suíte completa com ./mvnw verify: 90 testes,
  sem falhas, erros ou testes ignorados; BUILD SUCCESS.
- CI desta branch: pendente.

## Testes HTTP de criação

Cobertura adicionada para:

- Criação autenticada com resposta HTTP 201.
- Código retornado correspondente ao registro persistido.
- Proprietário obtido da autenticação.
- Status inicial ABERTO.
- Normalização de título e descrição.
- Tentativa de definir proprietário e status pelo corpo da requisição.
- Rejeição sem autenticação.
- Rejeição de campos obrigatórios inválidos.
- Rejeição de categoria desconhecida.
- Rejeição de textos acima dos limites.
- Rejeição de JSON malformado.
- Ausência de persistência nas entradas rejeitadas.

A política de rejeitar ou ignorar campos desconhecidos
ainda não foi fixada. Em ambos os casos, esses campos
não podem controlar proprietário ou status.

Execução dos testes HTTP: pendente.

## Próximas etapas

- Contrato de persistência e caso de uso.
- Modelo JPA, mapper, repositório e adaptador.
- Endpoint e DTOs.
- Testes de aplicação e integração HTTP.
- Atualização do README e execução da CI.