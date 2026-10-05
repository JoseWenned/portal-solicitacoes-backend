# Operações de solicitações

## Objetivo

Implementar edição, exclusão física e alteração de status,
limitadas ao proprietário da solicitação.

## Regras de domínio

- Edição somente no status ABERTO.
- Exclusão somente no status ABERTO.
- Transições permitidas: ABERTO → EM_ATENDIMENTO → CONCLUIDO.
- Repetição do status atual, retorno e salto de etapas são proibidos.
- Edição preserva identidade, código, proprietário e data de criação.
- Alteração de status preserva os dados descritivos.
- Edição e alteração de status atualizam updatedAt.
- A data da operação não pode preceder a última atualização.

A entidade permanece imutável e independente de frameworks.
Edição e alteração de status retornam uma nova instância
com a mesma identidade.

A validação da exclusão não altera a entidade.

OperacaoSolicitacaoInvalidaException representa conflito
com o estado atual e é traduzida para HTTP 409.

## Aplicação

- EditarSolicitacaoUseCase coordena a edição.
- ExcluirSolicitacaoUseCase coordena a exclusão física.
- AlterarStatusSolicitacaoUseCase coordena a transição de status.

Os casos de uso consultam a solicitação por ID e proprietário,
aplicam as regras de domínio e executam a escrita condicional.

Registro não encontrado para o proprietário resulta em 404.
Operação proibida pelo estado atual resulta em 409.

Quando a escrita afeta zero linhas após a leitura, o caso de uso
retorna conflito: o registro pode ter sido alterado ou excluído.

Clock permite controlar o instante das alterações nos testes.

## Persistência e controle de concorrência

As operações utilizam instruções condicionais no banco.

Todas verificam:

- Identificador da solicitação.
- Proprietário.
- Versão esperada.
- Status esperado.

A edição altera somente título, descrição, categoria e updatedAt.
A alteração de status modifica somente status e updatedAt.
Ambas incrementam version na mesma instrução.

A exclusão é física e exige status ABERTO.

O adaptador retorna true quando uma linha é afetada.
Retorna false quando o estado esperado não corresponde ao registro.

As instruções bulk incrementam a versão explicitamente,
sem depender do incremento automático de @Version.

Não há atualização genérica por save nessas operações.

A leitura e a escrita são operações separadas.
As condições de versão e status protegem o intervalo entre elas.

## Contrato HTTP

Todos os endpoints exigem Bearer JWT.
O proprietário é obtido exclusivamente do JWT validado.

### Edição

PUT /api/v1/solicitacoes/{id}

Entrada: titulo, descricao e categoria.
Todos os campos são obrigatórios.

### Exclusão

DELETE /api/v1/solicitacoes/{id}

Sem corpo. Remove fisicamente o registro.

### Alteração de status

PATCH /api/v1/solicitacoes/{id}/status

Entrada: status.

### Respostas

- 204: operação concluída, sem corpo.
- 400: dados ou parâmetros inválidos.
- 401: autenticação ausente ou inválida.
- 404: solicitação inexistente ou pertencente a outro usuário.
- 409: estado incompatível ou conflito de escrita.

Código do conflito: REQUEST_STATE_CONFLICT.

Após uma alteração, o cliente pode consultar novamente
a solicitação para obter seu estado atualizado.

## Estratégia de testes

### Domínio

Testes verificam edição, exclusão, transições permitidas,
transições proibidas, preservação dos campos e datas.

### HTTP

Quatorze execuções verificam:

- Edição de solicitação aberta.
- Preservação de proprietário, código e data de criação.
- Incremento da versão após edição e alteração de status.
- Exclusão física e consulta posterior com 404.
- Fluxo ABERTO → EM_ATENDIMENTO → CONCLUIDO.
- Recusa de edição e exclusão fora de ABERTO.
- Recusa de saltos, retornos e repetição de status.
- Recusa das três operações sobre solicitação de outro usuário.
- Registro inexistente e ausência de autenticação.
- Dados de edição inválidos.
- Status ausente ou desconhecido.

Cadastro, autenticação e criação são realizados por HTTP.
O banco temporário é consultado para confirmar os efeitos.

### Condições de escrita

Três testes de persistência verificam:

- Recusa das três operações com versão antiga.
- Recusa por status incompatível, mesmo com versão correspondente.
- Recusa por proprietário incorreto nas três operações.
- Preservação do registro após tentativas recusadas.
- Incremento da versão nas operações aceitas.

Os testes utilizam PostgreSQL temporário via Testcontainers,
separado do banco do Docker Compose.

## Validação confirmada

Resultados informados pelo desenvolvedor no Ubuntu/WSL:

- Regras de domínio das operações aprovadas.
- Quatorze execuções HTTP aprovadas.
- Três testes das condições de escrita aprovados.
- Suíte completa com ./mvnw verify: 141 testes.
- Nenhuma falha, erro ou teste ignorado.
- BUILD SUCCESS.

CI desta branch: pendente.

## Limites da validação

Os testes de persistência simulam expectativas desatualizadas
sequencialmente, sem executar múltiplas threads concorrentes.

O controle protege alterações entre a leitura e a escrita
realizadas pelo backend.

Não detecta, por si só, um formulário antigo no cliente:
a versão esperada é consultada pelo backend durante a operação.

## Próximas etapas

- Dashboard por usuário.
- Documentação OpenAPI.
- Revisão da integração e validação final do backend.