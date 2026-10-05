# Registro de apoio de LLM — operações de solicitações

## Objetivo

Apoiar edição, exclusão física e alteração de status,
com regras de propriedade, estado e controle de versão.

## Apoio recebido

- Ampliação da entidade Solicitacao.
- Proposta da exceção de operação inválida.
- Elaboração dos testes das regras de domínio.
- Ampliação da porta de persistência com operações específicas.
- Proposta de UPDATE e DELETE condicionais.
- Implementação dos casos de uso.
- Proposta dos DTOs e controller das operações.
- Tratamento HTTP 409 para conflitos.
- Elaboração dos testes HTTP.
- Elaboração dos testes das condições de escrita.
- Consolidação da documentação após a validação.

## Decisões adotadas

- Preservar a organização por camadas.
- Manter a entidade imutável e independente de frameworks.
- Permitir edição e exclusão somente em ABERTO.
- Permitir somente avanço para o próximo status.
- Preservar identidade e proprietário nas alterações.
- Realizar exclusão física.
- Obter o proprietário exclusivamente do JWT.
- Condicionar escritas ao proprietário, versão e status esperado.
- Incrementar explicitamente a versão nas atualizações bulk.
- Complementar a cobertura existente sem repetir testes de criação.

## Ajuste realizado durante a revisão

O adaptador de edição inicialmente reutilizava validarExclusao
para verificar o status.

A implementação foi corrigida para verificar explicitamente
o status ABERTO, com mensagem correspondente à edição.

A suíte continuou passando após a correção.

## Implementação validada

- Regras de edição, exclusão e transição de status.
- Operações específicas de persistência.
- Casos de uso de edição, exclusão e alteração de status.
- Endpoints PUT, DELETE e PATCH.
- Resposta 204 nas operações concluídas.
- Resposta 404 para registro inexistente ou alheio.
- Resposta 409 para estado incompatível ou conflito de escrita.
- Testes de domínio, HTTP e persistência.

## Estratégia de validação

Os testes HTTP utilizam cadastro, login e criação reais.

Os testes de persistência simulam versão antiga,
status incompatível e proprietário incorreto,
verificando que o registro permanece preservado.

As expectativas desatualizadas são simuladas sequencialmente,
sem execução de múltiplas threads.

## Evidências informadas pelo desenvolvedor

Execução no Ubuntu/WSL:

- Quatorze execuções HTTP das operações aprovadas.
- Três testes das condições de escrita aprovados.
- Suíte completa com ./mvnw verify: 141 testes.
- Nenhuma falha, erro ou teste ignorado.
- BUILD SUCCESS.

Os resultados foram apresentados pelo desenvolvedor.
Não representam execução independente pela LLM.

## Limitações registradas

A versão esperada é consultada pelo backend durante a operação.
Não há detecção específica de formulários antigos no cliente.

A cobertura não simula múltiplas threads concorrentes.

## Pendências

- Execução da CI desta branch.
- Dashboard, OpenAPI e validação final do backend
  em etapas posteriores.