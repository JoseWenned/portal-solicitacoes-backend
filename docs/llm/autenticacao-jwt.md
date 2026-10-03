# Registro de apoio de LLM — autenticação JWT

## Objetivo

Registrar o apoio de LLM na implementação e validação
da autenticação com JWT e sessões revogáveis.

## Apoio recebido

- Proposta da entidade de sessão e seus testes.
- Ampliação das consultas de usuário e da verificação de senha.
- Definição dos contratos de persistência e tokens.
- Proposta do modelo, mapper, repositório JPA e adaptador.
- Implementação de rotação atômica e revogação idempotente.
- Elaboração dos testes de integração da persistência.
- Proposta dos casos de uso e seus dez testes unitários.
- Implementação dos provedores de refresh token e JWT.
- Elaboração dos testes dos provedores.
- Configuração da composição e da chave externa.
- Implementação proposta de segurança HTTP, cookies e CSRF.
- Proposta dos endpoints de autenticação e usuário autenticado.
- Apoio no diagnóstico de configuração e execução.
- Revisão e atualização da documentação.

## Decisões do desenvolvedor

- Manter a organização por camadas.
- Separar modelos de persistência das entidades de domínio.
- Utilizar mappers específicos.
- Preservar a validade absoluta da sessão durante a rotação.
- Impedir renovação de sessões expiradas ou revogadas.
- Utilizar operações específicas de atualização.
- Manter a chave JWT fora do código.
- Revisar as propostas e executar as verificações antes de avançar.
- Atualizar a documentação durante o desenvolvimento.

## Implementação atual

- Domínio e persistência da sessão.
- Consulta de usuário e verificação BCrypt da senha.
- Casos de uso de login, renovação e logout.
- Geração aleatória do refresh token e hash SHA-256.
- Emissão de JWT com HS256.
- Composição das dependências pelo Spring.
- Configuração externa da chave JWT.
- Decoder com validação de claims e sessão.
- Endpoints de autenticação e consulta do usuário.
- Cookies e proteção CSRF.

A integração HTTP foi compilada e inicializada.
Os fluxos completos de login, renovação e logout
ainda aguardam validação por HTTP.

## Problemas identificados e correções

### Variável no serviço incorreto

JWT_SECRET_BASE64 havia sido configurada no serviço database.
Foi transferida para o serviço backend.

### Perfil de teste ausente

As classes de integração não ativavam o perfil test.
Foi criado application-test.yaml com chave exclusiva de teste
e adicionada ActiveProfiles nas três classes de integração.

Após a correção, os 45 testes então existentes voltaram a passar.

### Script executado fora do projeto

O script de configuração da chave foi executado na pasta pessoal,
onde não existia o arquivo .env.

A execução foi repetida na raiz do backend.
A presença da variável foi conferida sem exibir seu valor.

### Inicialização do container

O endpoint de saúde apresentou resets durante a inicialização.
As tentativas posteriores retornaram status UP.

## Evidências informadas pelo desenvolvedor

- Domínio da sessão: sete testes aprovados.
- Persistência da sessão: seis testes de integração aprovados.
- Provedores de tokens: quatro testes aprovados.
- Casos de uso de autenticação: dez testes aprovados.
- Suíte completa após a integração HTTP: 55 testes,
  sem falhas, erros ou ignorados.
- Maven verify: BUILD SUCCESS.
- Backend iniciado pelo Docker Compose.
- PostgreSQL healthy.
- Endpoint de saúde: status UP.
- Consulta do usuário sem JWT: HTTP 401.
- Obtenção de CSRF: HTTP 200, com token no JSON
  e cookie HttpOnly e SameSite=Lax.

As evidências foram apresentadas pelo desenvolvedor
a partir da execução no Ubuntu/WSL.

## Limites da validação

- Os testes de persistência não simulam múltiplas threads concorrentes.
- A suíte atual testa os casos de uso, mas ainda não verifica
  o fluxo completo de autenticação por HTTP.
- A saúde da aplicação não comprova login, renovação ou logout.
- Renovação e logout concorrentes ainda precisam de tratamento.

## Validação HTTP e suíte completa

- Testes HTTP de autenticação: 5 executados, sem falhas,
  erros ou testes ignorados.
- Fluxo de login, renovação e logout validado por HTTP.
- Rotação confirmada pela alteração do JWT e do hash
  do refresh token persistido.
- Logout confirmado pela revogação da sessão e rejeição
  dos access tokens emitidos anteriormente.
- Operações de autenticação sem CSRF retornam HTTP 403.
- Senha incorreta retorna HTTP 401 sem criar sessão.
- Suíte completa com ./mvnw verify: 60 testes, sem falhas,
  erros ou testes ignorados; BUILD SUCCESS.
- CI desta branch: pendente.

## Próximas etapas

- Adicionar e executar testes HTTP de autenticação.
- Verificar invalidação do JWT após logout.
- Validar rejeição de operações sem CSRF.
- Tratar a coordenação entre renovação e logout.
- Unificar respostas de erro.
- Atualizar README e Memorial Técnico.
- Executar CI e preparar commit e PR.

O teste HTTP proposto ainda não possui resultado de execução
e não é contabilizado nas evidências atuais.