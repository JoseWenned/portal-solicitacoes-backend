# Dicionário de dados

## Objetivo

Descrever as tabelas, os campos, as restrições, os relacionamentos
e os índices do Portal de Solicitações Internas.

Este documento corresponde às migrations V1, V2 e V3 presentes
em src/main/resources/db/migration.

## Convenções

- Banco de dados: PostgreSQL 16.
- Identificadores técnicos: UUID.
- Datas persistidas como TIMESTAMPTZ.
- Comunicação temporal da aplicação configurada em UTC.
- Filtros por datas interpretados no fuso America/Sao_Paulo.
- Estrutura versionada pelo Flyway.
- Hibernate configurado com ddl-auto=validate.

TIMESTAMPTZ representa instantes e não preserva o nome do fuso
originalmente informado. Sua apresentação depende do fuso da sessão
do PostgreSQL.

## Tabela usuarios

Armazena os usuários cadastrados e os hashes de suas senhas.

| Campo | Tipo | Obrigatório | Valor padrão | Descrição |
|---|---|---|---|---|
| id | UUID | Sim | Sem padrão | Identificador gerado pela aplicação; chave primária |
| name | VARCHAR(100) | Sim | Sem padrão | Nome público |
| email | VARCHAR(254) | Sim | Sem padrão | E-mail normalizado, único e utilizado no login |
| password_hash | VARCHAR(255) | Sim | Sem padrão | Hash BCrypt da senha |
| created_at | TIMESTAMPTZ | Sim | CURRENT_TIMESTAMP | Instante de criação |

### Restrições

| Restrição | Finalidade |
|---|---|
| PRIMARY KEY de id | Identifica unicamente o usuário |
| uk_usuarios_email | Impede e-mails duplicados |
| ck_usuarios_name | Exige ao menos três caracteres após btrim |
| ck_usuarios_email_normalizado | Exige e-mail não vazio, em minúsculas e sem espaços externos removidos por btrim |

Os limites máximos de name e email são definidos pelos tipos VARCHAR.

### Regras adicionais da aplicação

- Nome entre 3 e 100 pontos de código Unicode após strip.
- E-mail com formato válido.
- Normalização do e-mail antes da persistência.
- Senha com pelo menos 8 pontos de código Unicode.
- Senha com no máximo 72 bytes em UTF-8.
- Senha sem remoção de espaços ou normalização.
- Hash BCrypt com custo 12.
- Senha e hash não são retornados pela API.

Java strip e PostgreSQL btrim não possuem exatamente a mesma
abrangência de caracteres de espaço.

A consulta prévia de e-mail não substitui a restrição única,
que também protege contra cadastros simultâneos.

## Tabela solicitacoes

Armazena as solicitações e seu estado atual.

| Campo | Tipo | Obrigatório | Valor padrão | Descrição |
|---|---|---|---|---|
| id | UUID | Sim | Sem padrão | Identificador técnico gerado pelo domínio; chave primária |
| codigo | BIGINT GENERATED ALWAYS AS IDENTITY | Sim, pela identidade | Gerado pelo PostgreSQL | Código numérico público |
| titulo | VARCHAR(150) | Sim | Sem padrão | Título da solicitação |
| descricao | VARCHAR(5000) | Sim | Sem padrão | Descrição da demanda |
| categoria | VARCHAR(30) | Sim | Sem padrão | Categoria |
| status | VARCHAR(20) | Sim | ABERTO | Estado atual |
| solicitante_id | UUID | Sim | Sem padrão | Referência a usuarios.id |
| created_at | TIMESTAMPTZ | Sim | CURRENT_TIMESTAMP | Instante de criação |
| updated_at | TIMESTAMPTZ | Sim | CURRENT_TIMESTAMP | Instante da última atualização |
| version | BIGINT | Sim | 0 | Versão utilizada nas condições de escrita |

O código é único, mas não possui garantia de sequência sem lacunas.
Ele não substitui o UUID como identificador técnico.

### Categorias permitidas

- TI
- RH
- COMPRAS
- FINANCEIRO
- INFRAESTRUTURA

### Status permitidos

- ABERTO
- EM_ATENDIMENTO
- CONCLUIDO

### Restrições

| Restrição | Finalidade |
|---|---|
| PRIMARY KEY de id | Identifica unicamente a solicitação |
| uk_solicitacoes_codigo | Garante unicidade do código |
| fk_solicitacoes_usuario | Exige proprietário existente |
| ck_solicitacoes_titulo | Impede título vazio após btrim |
| ck_solicitacoes_descricao | Impede descrição vazia após btrim |
| ck_solicitacoes_categoria | Limita os valores de categoria |
| ck_solicitacoes_status | Limita os valores de status |
| ck_solicitacoes_version | Impede versão negativa |

### Índices

| Índice | Colunas | Finalidade |
|---|---|---|
| idx_solicitacoes_usuario_data | solicitante_id, created_at DESC, codigo DESC | Apoia listagem por proprietário e ordenação |
| idx_solicitacoes_usuario_status | solicitante_id, status | Apoia buscas por proprietário e status |
| idx_solicitacoes_usuario_categoria | solicitante_id, categoria | Apoia buscas por proprietário e categoria |

Não há índice específico para busca parcial por título.
O uso efetivo dos índices depende do plano escolhido pelo PostgreSQL.

### Regras adicionais da aplicação

- Proprietário obtido exclusivamente da autenticação.
- Status inicial ABERTO.
- Edição e exclusão permitidas somente em ABERTO.
- Transições permitidas: ABERTO → EM_ATENDIMENTO → CONCLUIDO.
- Exclusão física.
- Edição preserva identidade, código, proprietário e criação.
- Alteração de status preserva os campos descritivos.
- Atualizações incrementam version e alteram updated_at.
- A data da operação não pode preceder a última atualização.
- Consultas, filtros e totais são limitados ao proprietário.

O CHECK de status valida os valores possíveis, mas não determina
a sequência de transições.

Não há trigger para atualizar updated_at ou version.
Esses campos são modificados pelas operações explícitas do backend.

A versão protege o intervalo entre leitura e escrita no backend.
Não é exposta no contrato atual para detectar formulários antigos.

## Tabela sessoes_autenticacao

Armazena as sessões revogáveis e o hash do refresh token atual.

| Campo | Tipo | Obrigatório | Valor padrão | Descrição |
|---|---|---|---|---|
| id | UUID | Sim | Sem padrão | Identificador da sessão; chave primária |
| usuario_id | UUID | Sim | Sem padrão | Referência a usuarios.id |
| refresh_token_hash | VARCHAR(64) | Sim | Sem padrão | Hash SHA-256 hexadecimal do refresh token |
| created_at | TIMESTAMPTZ | Sim | CURRENT_TIMESTAMP | Instante de criação |
| expires_at | TIMESTAMPTZ | Sim | Sem padrão | Expiração absoluta |
| revoked_at | TIMESTAMPTZ | Não | NULL | Instante da revogação |
| version | BIGINT | Sim | 0 | Versão utilizada no controle de atualização |

O refresh token original não é armazenado.

### Restrições

| Restrição | Finalidade |
|---|---|
| PRIMARY KEY de id | Identifica unicamente a sessão |
| fk_sessoes_usuario | Exige usuário existente |
| uk_sessoes_refresh_token_hash | Impede duplicidade do hash atual |
| ck_sessoes_refresh_token_hash | Exige 64 caracteres hexadecimais em minúsculas |
| ck_sessoes_expiracao | Exige expiração posterior à criação |
| ck_sessoes_revogacao | Exige revogação nula ou não anterior à criação |
| ck_sessoes_version | Impede versão negativa |

### Índices

| Índice | Colunas | Finalidade |
|---|---|---|
| idx_sessoes_usuario | usuario_id | Apoia consultas por usuário |
| idx_sessoes_expiracao | expires_at | Apoia consultas por expiração |

A restrição única do hash também cria um índice único.

### Regras adicionais da aplicação

- Validade absoluta de oito horas.
- Sessão inativa no instante da expiração ou após revogação.
- Rotação substitui o hash sem estender expires_at.
- Rotação verifica versão, hash anterior e estado ativo.
- Revogação preserva a primeira data de encerramento.
- JWT identifica usuário e sessão.
- A sessão é verificada em cada acesso protegido.

As restrições do banco não definem a duração de oito horas
nem removem automaticamente sessões expiradas.

## Relacionamentos

- Um usuário pode possuir várias solicitações.
- Uma solicitação pertence a um usuário.
- Um usuário pode possuir várias sessões.
- Uma sessão pertence a um usuário.

As chaves estrangeiras não possuem ON DELETE CASCADE.
Não há funcionalidade de exclusão de usuários no escopo atual.

## Índices de integridade

Chaves primárias e restrições UNIQUE criam índices próprios,
além dos índices explicitamente declarados nas migrations.

## Histórico de migrations

| Versão | Arquivo | Finalidade |
|---|---|---|
| V1 | V1__criar_tabela_usuarios.sql | Criação de usuários |
| V2 | V2__criar_tabela_solicitacoes.sql | Criação de solicitações |
| V3 | V3__criar_tabela_sessoes_autenticacao.sql | Criação de sessões |

O Flyway mantém a tabela técnica flyway_schema_history para registrar
as migrations aplicadas e verificar sua integridade.

Migrations já aplicadas devem ser preservadas.
Mudanças posteriores devem utilizar novas migrations.

## Limitações e melhorias futuras

- Não há auditoria histórica das alterações de solicitações.
- Não há limpeza automática de sessões expiradas ou revogadas.
- Não há índice específico para busca textual.
- Regras de transição e propriedade dependem da aplicação.
- Não há exclusão de usuários nem política de retenção implementada.
- Backup e restauração não foram automatizados.

## Evidências de validação

- Inicialização com Flyway e Hibernate validate.
- Migrations V1, V2 e V3 informadas como aplicadas com sucesso.
- Persistência verificada com PostgreSQL temporário via Testcontainers.
- Código gerado pelo banco verificado nos testes.
- Condições de proprietário, versão e status verificadas nas escritas.
- Rotação e revogação das sessões verificadas.
- Suíte completa: 153 testes, sem falhas, erros ou testes ignorados.
- Maven verify: BUILD SUCCESS.

Resultados informados pelo desenvolvedor a partir da execução
no Ubuntu/WSL.