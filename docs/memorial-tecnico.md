# Memorial Técnico de Desenvolvimento

## 1. Identificação

**Projeto:** Portal de Solicitações Internas.

**Autor:** Wenned Chaves.

**Finalidade:** mini-projeto Full Stack desenvolvido para o processo seletivo
de Desenvolvedor de Sistemas Júnior da bit Soluções.

**Data desta versão:** 5 de outubro de 2026.

**Backend:**
https://github.com/JoseWenned/portal-solicitacoes-backend

**Frontend:**
https://github.com/JoseWenned/portal-solicitacoes-frontend

**Estado:** funcionalidades implementadas e validadas localmente.
Conferência documental e confirmação das execuções finais da CI
fazem parte do fechamento da entrega.

O desenvolvimento foi realizado no Ubuntu, utilizando WSL
em uma máquina Windows.

Este memorial registra decisões, justificativas, arquitetura,
processo de desenvolvimento, evidências e limitações da solução.

## 2. Interpretação do problema

O portal permite que usuários registrem demandas internas,
acompanhem sua evolução e consultem indicadores.

O escopo contempla:

- Cadastro de usuário.
- Autenticação, controle de sessão e logout.
- Criação e consulta de solicitações.
- Listagem paginada.
- Edição e exclusão de solicitações abertas.
- Alteração de status.
- Filtros por período, categoria, status e título.
- Dashboard com total e contagens por status.
- Backend, frontend e persistência SQL.
- Instruções de configuração e execução.
- Scripts de criação da estrutura e dicionário de dados.
- README e Memorial Técnico de Desenvolvimento.

Docker, testes automatizados, integração contínua
e responsividade foram incluídos como diferenciais.

Docker, testes e CI foram introduzidos desde as primeiras etapas.

A solução não possui publicação automática de imagens
nem deploy automatizado.

## 3. Decisões de escopo

As decisões adotadas durante a análise foram:

- Cadastro com name, email e password.
- Login por e-mail e senha.
- Usuários de demonstração criados pela tela de cadastro.
- Acesso somente às próprias solicitações.
- Solicitante obtido exclusivamente da autenticação.
- Status inicial ABERTO.
- Fluxo ABERTO → EM_ATENDIMENTO → CONCLUIDO.
- Edição e exclusão somente em ABERTO.
- Exclusão física.
- Dashboard restrito ao usuário autenticado.

O e-mail concretiza o identificador de usuário utilizado no login.

O isolamento por proprietário é uma decisão explícita de escopo.

Não foram implementados papéis administrativos, atendimento
por outro usuário, recuperação de senha ou verificação de e-mail.

## 4. Organização do desenvolvimento

Backend e frontend foram mantidos em repositórios separados.

Essa decisão permite organizar dependências, builds,
testes e workflows de forma independente.

Como contrapartida, a execução integrada exige coordenar
a configuração dos dois repositórios.

Foram utilizadas branches por funcionalidade,
commits descritivos e pull requests para integração à main.

A documentação acompanhou as implementações, incluindo:

- Arquitetura e decisões técnicas.
- Regras de negócio.
- Contratos HTTP.
- Persistência e migrations.
- Dicionário de dados.
- Configuração de infraestrutura.
- Evidências dos testes.
- Limitações.
- Registros do apoio recebido de LLM.

Os documentos foram revisados conforme novas evidências
substituíram informações anteriormente pendentes.

## 5. Tecnologias do backend e justificativas

As versões das dependências estão registradas no pom.xml
e na configuração do Maven Wrapper.

As escolhas descritas são adequadas ao contexto deste projeto,
sem representar superioridade universal sobre as alternativas.

### Java 21

**Motivo:** linguagem estudada pelo desenvolvedor, com tipagem estática
e recursos adequados às regras e contratos do backend.

**Benefícios:** verificação de tipos, ferramentas de desenvolvimento,
testes e uma versão LTS da linguagem.

**Alternativas:** C# com ASP.NET Core ou JavaScript/TypeScript
com Node.js também atenderiam ao projeto.

**Impacto:** favorece contratos explícitos e manutenção,
com necessidade de JVM e maior estrutura inicial.

### Spring Boot 4.1.1 e Spring Web MVC

**Motivo:** implementar uma API HTTP com configuração integrada.

**Benefícios:** integração entre injeção de dependências,
servidor HTTP, validação, persistência, segurança e testes.

**Alternativas:** configuração manual do Spring ou outro framework Java.

**Impacto:** reduz configuração inicial e favorece produtividade,
mas exige conhecimento das convenções e compatibilidade
das dependências.

O modelo síncrono do Spring Web MVC foi considerado suficiente.

Uma arquitetura reativa acrescentaria complexidade
sem necessidade demonstrada no escopo.

### Maven e Maven Wrapper

**Motivo:** padronizar dependências, build, testes e empacotamento.

**Benefícios:** o Wrapper utiliza a versão definida no projeto
sem instalação global do Maven.

**Alternativa:** Gradle.

**Impacto:** favorece reprodução local e na CI.
A primeira execução exige download das dependências.

Versão configurada: Maven 3.9.16.

Comando principal de verificação:

```bash
./mvnw verify
```

### PostgreSQL 16

**Motivo:** persistir dados relacionais com integridade referencial.

**Benefícios:** transações, restrições, UUID, TIMESTAMPTZ,
índices e consultas agregadas.

**Alternativas:** MySQL ou MariaDB.

**Impacto:** permite executar filtros e agregações no banco
e proteger integridade com restrições.

Em produção, exigiria backup, monitoramento
e planejamento de capacidade.

### Spring Data JPA e Hibernate

**Motivo:** integrar mapeamento, consultas e paginação ao Spring.

**Benefícios:** repositórios e composição de filtros com Specification.

**Alternativa:** JDBC em todas as operações, com mais controle
do SQL e maior quantidade de mapeamento manual.

**Impacto:** reduz repetição, mas exige atenção às consultas,
transações e contexto de persistência.

Os modelos JPA são separados das entidades de domínio.

Configurações adotadas:

- open-in-view desabilitado.
- ddl-auto configurado como validate.
- Fuso de trabalho do Hibernate configurado em UTC.

As operações de alteração possuem métodos específicos,
sem atualização genérica por save.

### JdbcTemplate

**Motivo:** calcular o dashboard em uma consulta agregada.

**Benefícios:** SQL explícito, sem carregar todas as solicitações
em memória.

**Alternativa:** consultas agregadas por JPA.

**Impacto:** simplifica essa operação específica,
mantendo a dependência do SQL na infraestrutura.

### Flyway

**Motivo:** versionar a estrutura do banco junto do código.

**Benefícios:** migrations ordenadas e histórico de execução.

**Alternativa:** Liquibase.

**Impacto:** favorece reprodução e revisão das mudanças.

O Flyway cria e evolui o esquema.
O Hibernate valida a correspondência do mapeamento.

Migrations já aplicadas devem ser preservadas.
Alterações posteriores devem utilizar novas migrations.

### Bean Validation

**Motivo:** validar os dados recebidos pelos controllers.

**Benefícios:** validação declarativa e identificação
dos campos inválidos.

**Alternativa:** validação manual em todos os controllers.

**Impacto:** melhora a consistência das entradas,
sem substituir regras de domínio e restrições do banco.

### Spring Security e OAuth2 Resource Server

**Motivo:** proteger endpoints e validar Bearer JWT.

**Benefícios:** cadeia de filtros, autenticação HTTP,
validação de tokens e proteção CSRF.

**Alternativa:** filtros próprios.

**Impacto:** centraliza a segurança HTTP, enquanto regras
de propriedade e estado permanecem na aplicação.

### JWT e sessões revogáveis

**Motivo:** utilizar access tokens curtos e permitir revogação
por estado persistido.

**Benefícios:** identificação do usuário e da sessão,
com separação entre access token e refresh token.

**Alternativa:** sessão tradicional identificada por cookie,
que também atenderia ao portal.

**Impacto:** exige coordenação entre tokens, cookies,
renovação, revogação e CSRF.

A sessão é consultada no banco durante os acessos protegidos.
Portanto, a solução mantém estado.

JWT foi uma preferência do desenvolvedor.
Seu uso isolado não determina a qualidade da solução.

### BCrypt

**Motivo:** proteger senhas com algoritmo próprio
para armazenamento de senhas.

**Benefícios:** salt e custo configurável.

**Alternativa:** Argon2.

**Impacto:** o custo 12 aumenta o trabalho de verificação.

A entrada é limitada a 72 bytes em UTF-8,
conforme a política adotada para BCrypt.

### SecureRandom e SHA-256

**Motivo:** gerar refresh tokens aleatórios
e persistir somente seus hashes.

**Benefícios:** geração de 32 bytes aleatórios
e armazenamento de hash SHA-256 hexadecimal.

**Alternativa:** outro mecanismo seguro de geração de tokens
e identificação das credenciais.

**Impacto:** permite consulta e rotação sem armazenar
o refresh token original.

SHA-256 é utilizado para tokens aleatórios.
As senhas são protegidas por BCrypt.

### springdoc-openapi 3.1.1 e Swagger UI

**Motivo:** disponibilizar o contrato HTTP e sua exploração interativa.

**Benefícios:** consulta aos endpoints, parâmetros,
entradas e respostas.

**Alternativa:** especificação OpenAPI mantida manualmente.

**Impacto:** facilita a comunicação do contrato,
mas exige revisão das respostas e regras documentadas.

Contrato gerado observado: OpenAPI 3.1.0.

### Actuator

**Motivo:** fornecer endpoint de saúde.

**Benefícios:** verificação padronizada da disponibilidade.

**Alternativa:** endpoint próprio.

**Impacto:** auxilia execução e diagnóstico.

Somente os endpoints de saúde necessários são expostos,
sem detalhes internos na resposta pública.

Status UP não comprova todos os fluxos de negócio.

### JUnit e Mockito

**Motivo:** verificar entidades e casos de uso.

**Benefícios:** cenários repetíveis e dependências controladas.

**Alternativa:** outras ferramentas de teste da plataforma Java.

**Impacto:** favorecem manutenção das regras.

Mocks são complementados por testes com integração real.

### Spring Boot Test e Testcontainers

**Motivo:** testar contexto, HTTP e persistência
com PostgreSQL temporário.

**Benefícios:** maior fidelidade ao banco utilizado pela aplicação
e isolamento do banco do Docker Compose.

**Alternativa:** H2.

**Impacto:** aumenta a confiança na integração,
com maior tempo de execução e dependência do Docker.

### JsonPath e AssertJ

**Motivo:** consultar respostas JSON
e escrever verificações legíveis.

**Benefícios:** acesso aos campos do contrato
e mensagens de falha claras.

**Alternativa:** leitura manual do JSON e assertions básicas.

**Impacto:** simplifica a manutenção dos testes HTTP.

## 6. Tecnologias do frontend e justificativas

As dependências e versões estão registradas no package.json
e no package-lock.json.

### JavaScript e React

**Motivo:** construir a interface com componentes
e atualização coordenada pelo estado da aplicação.

**Benefícios:** reutilização de componentes e organização
dos fluxos de interação.

**Alternativas:** Vue ou Angular.
TypeScript poderia substituir JavaScript na implementação.

**Impacto:** favorece produtividade e composição da interface.

JavaScript foi adotado conforme a stack escolhida.
TypeScript é uma possível evolução para ampliar
a verificação estática dos contratos.

### Vite

**Motivo:** organizar o servidor de desenvolvimento
e o build do frontend.

**Benefícios:** configuração compacta e geração
dos arquivos estáticos de produção.

**Alternativas:** outras ferramentas de build ou frameworks
com recursos adicionais, como Next.js.

**Impacto:** mantém o frontend como aplicação no navegador,
sem exigir renderização no servidor.

O proxy de desenvolvimento encaminha /api ao backend.

### SCSS

**Motivo:** organizar os estilos da interface.

**Benefícios:** estruturação dos estilos e controle
da apresentação responsiva.

**Alternativas:** CSS puro ou biblioteca de componentes.

**Impacto:** permite personalizar a interface,
com responsabilidade pela consistência dos estilos.

### React Router

**Motivo:** organizar a navegação entre as páginas.

**Benefícios:** rotas declarativas e composição
de rotas protegidas.

**Alternativa:** outra solução de roteamento.

**Impacto:** separa navegação e conteúdo,
permitindo acesso direto às páginas.

A proteção das rotas melhora a experiência.
A autorização efetiva permanece no backend.

### Axios

**Motivo:** centralizar a comunicação HTTP.

**Benefícios:** configuração reutilizável, interceptadores,
timeout e tratamento de respostas.

**Alternativa:** Fetch API.

**Impacto:** reduz repetição nas chamadas
e permite coordenar a renovação da autenticação.

A configuração inclui:

- URL base /api/v1.
- Envio de cookies.
- Inclusão do access token.
- Tratamento de respostas 401.
- Timeout das requisições.

### React Hook Form e Zod

**Motivo:** gerenciar formulários e validar entradas.

**Benefícios:** mensagens por campo e esquemas explícitos
para os dados recebidos.

**Alternativas:** validação manual ou outras bibliotecas
de formulários e esquemas.

**Impacto:** melhora a experiência e reduz repetição.

A integração utiliza @hookform/resolvers.

A validação no frontend não substitui
as verificações do backend e do banco.

### Context API

**Motivo:** compartilhar autenticação entre os componentes.

**Benefícios:** utiliza recursos do React
sem acrescentar uma biblioteca de estado global.

**Alternativa:** Redux.

**Impacto:** atende ao estado compartilhado limitado
do projeto com menor configuração.

Os dados das solicitações são carregados pelas páginas,
sem concentrar todos os dados em estado global.

A decisão pode ser reavaliada se os fluxos compartilhados
se tornarem mais complexos.

### Vitest e React Testing Library

**Motivo:** verificar casos de uso e comportamento dos formulários.

**Benefícios:** integração com o ambiente Vite
e testes por interações acessíveis ao usuário.

**Alternativa:** Jest.

**Impacto:** permite verificações rápidas e repetíveis
durante o desenvolvimento.

### Playwright

**Motivo:** verificar o fluxo integrado no navegador.

**Benefícios:** execução contra frontend e backend reais,
com projetos desktop e emulação móvel.

**Alternativa:** Cypress.

**Impacto:** complementa os testes isolados,
identificando problemas entre interface, API e autenticação.

A emulação móvel não substitui dispositivos físicos
nem revisão visual manual.

### ESLint

**Motivo:** identificar problemas no código
e no uso de React Hooks.

**Benefícios:** verificação automática durante o desenvolvimento
e na integração contínua.

**Alternativa:** outra ferramenta de análise estática.

**Impacto:** auxilia a manutenção,
sem comprovar o funcionamento dos fluxos.

### Node.js e npm

**Motivo:** instalar dependências e executar
as ferramentas do frontend.

**Benefícios:** scripts padronizados e instalação
reproduzível por npm ci.

**Alternativas:** outros gerenciadores de pacotes.

**Impacto:** o package-lock.json registra a resolução
das dependências.

O ambiente utiliza Node.js 22.

Node.js é necessário para desenvolvimento e build.
A interface final é servida pelo Nginx.

## 7. Ferramentas de infraestrutura e desenvolvimento

### Docker e Docker Compose

**Motivo:** executar aplicações e banco
com configuração explícita.

**Benefícios:** isolamento e reprodução do ambiente.

**Alternativa:** instalação manual de todos os componentes.

**Impacto:** facilita a execução pelo avaliador,
com dependência do daemon Docker e das imagens.

### Nginx

**Motivo:** servir os arquivos estáticos do frontend
e encaminhar chamadas da API.

**Benefícios:** entrega da interface e proxy
sob a mesma origem do navegador.

**Alternativas:** outro servidor HTTP ou serviço de hospedagem
com suporte a proxy.

**Impacto:** exige configuração do fallback das rotas React
e do encaminhamento ao backend.

O container utiliza uma imagem Nginx sem privilégios
administrativos.

### Git, GitHub e GitHub Actions

**Motivo:** versionar o projeto, revisar alterações
e automatizar verificações.

**Benefícios:** histórico, branches, pull requests
e integração contínua.

**Alternativas:** outros serviços Git e pipelines.

**Impacto:** melhora a rastreabilidade.

Os workflows não publicam imagens nem realizam deploy.

### Ubuntu no WSL

**Motivo:** utilizar ferramentas Linux na máquina Windows.

**Benefícios:** terminal e integração com Java, Git e Docker.

**Alternativa:** desenvolvimento diretamente no Windows.

**Impacto:** mantém o ambiente familiar ao desenvolvedor.

Os containers reduzem a dependência desse ambiente
para a execução pelo avaliador.

### Python 3

**Motivo:** gerar a chave JWT local com script auxiliar.

**Benefícios:** bibliotecas padrão e geração
sem imprimir o segredo.

**Alternativa:** outra ferramenta de geração aleatória segura.

**Impacto:** participa somente da configuração.
Não é dependência da aplicação.

### ChatGPT

**Motivo:** apoiar decisões, propostas de código,
diagnóstico e documentação.

**Benefícios:** agiliza a elaboração e comparação de propostas.

**Alternativas:** documentação oficial, pesquisa
e implementação sem assistência.

**Impacto:** exige revisão humana e execução
das verificações.

Não é dependência de execução da aplicação.

## 8. Arquitetura da solução

A solução possui:

- Frontend React.
- API Spring Boot.
- Banco PostgreSQL.

Frontend e backend comunicam-se por HTTP,
utilizando JSON.

O backend concentra regras de negócio, autorização
e persistência.

O frontend apresenta os dados e coordena
as interações do usuário.

### Organização do backend

| Camada | Responsabilidade |
|---|---|
| domain | Entidades, invariantes e exceções de negócio |
| application | Casos de uso, portas e resultados |
| infrastructure | Persistência, segurança, mappers e configuração |
| presentation | Controllers, DTOs e tratamento de erros HTTP |

Dentro das camadas, os arquivos são agrupados
por responsabilidade e conceito de negócio.

Domínio e aplicação não dependem de Spring ou JPA.

As portas descrevem dependências dos casos de uso.
Adaptadores implementam essas portas.
Configurações Spring conectam as implementações.

Os modelos de persistência utilizam a nomenclatura Model.

Mappers explícitos realizam conversões
entre domínio e persistência.

O resultado de paginação da aplicação
não utiliza tipos do Spring Data.

### Organização do frontend

| Camada | Responsabilidade |
|---|---|
| domain | Conceitos e regras utilizadas pela interface |
| application | Casos de uso e coordenação das operações |
| infrastructure | Axios, repositórios e gerenciamento da autenticação |
| presentation | Páginas, componentes, contexto, rotas e estilos |

Os casos de uso recebem dependências
para permitir testes com repositórios substitutos.

Alguns casos de uso utilizam esquemas Zod.

Portanto, a aplicação frontend possui dependência
da biblioteca de validação.

Essa característica difere do isolamento de Spring
e JPA mantido no domínio e na aplicação backend.

### Clean Architecture e SOLID

A separação busca manter regras e coordenação
independentes dos detalhes de persistência e HTTP.

Exemplos aplicados:

- Responsabilidades separadas entre controllers,
  casos de uso e adaptadores.
- Dependências dos casos de uso descritas por contratos.
- Composição das implementações fora do domínio.
- Operações específicas para alterações de estado.

Não foram criadas abstrações apenas
para demonstrar cada princípio.

### Conceitos de DDD

Foram utilizados:

- Identidade por UUID.
- Vocabulário de negócio explícito.
- Regras próximas às entidades.
- Criação e reconstituição controladas.
- Preservação das invariantes.

Esses conceitos não representam implementação
de todos os padrões estratégicos e táticos de DDD.

Microsserviços não foram considerados necessários
para o escopo.

## 9. Modelagem e persistência

Tabelas de negócio:

- usuarios.
- solicitacoes.
- sessoes_autenticacao.

Um usuário pode possuir várias solicitações
e sessões de autenticação.

UUID identifica tecnicamente os registros.

Solicitações também possuem código numérico
gerado pelo PostgreSQL.

### usuarios

Armazena:

- id.
- name.
- email.
- password_hash.
- created_at.

O e-mail possui restrição única.

A restrição de nome exige pelo menos três caracteres
após btrim.

O e-mail deve estar normalizado para minúsculas,
sem espaços externos e não vazio.

### solicitacoes

Armazena:

- id.
- codigo.
- titulo.
- descricao.
- categoria.
- status.
- solicitante_id.
- created_at.
- updated_at.
- version.

O proprietário é uma chave estrangeira para usuarios.

Restrições protegem categorias, status,
conteúdo obrigatório e versão não negativa.

Índices apoiam consultas por proprietário,
data, status e categoria.

O código é único, mas não representa uma sequência
obrigatoriamente sem lacunas.

### sessoes_autenticacao

Armazena:

- id.
- usuario_id.
- refresh_token_hash.
- created_at.
- expires_at.
- revoked_at.
- version.

O hash atual do refresh token possui restrição única.

As restrições verificam formato do hash,
coerência das datas e versão não negativa.

O token original não é armazenado.

### Evolução do esquema

As migrations V1, V2 e V3 criam a estrutura.

Flyway aplica as migrations.
Hibernate valida o mapeamento.

O detalhamento está disponível no:

[Dicionário de dados](database/dicionario-dados.md)

## 10. Cadastro de usuário

O cadastro recebe name, email e password.

Regras:

- Nome com 3 a 100 pontos de código Unicode
  após remover espaços externos.
- E-mail válido, normalizado para minúsculas e único.
- Senha com pelo menos 8 pontos de código Unicode.
- Senha com no máximo 72 bytes em UTF-8.
- Senha sem normalização ou remoção de espaços.
- Hash BCrypt com custo 12.

A consulta prévia de e-mail melhora o tratamento
do conflito.

A restrição única no banco protege contra cadastros
simultâneos que ultrapassem a consulta inicial.

A resposta retorna somente dados públicos.

O cadastro não autentica automaticamente o usuário.

## 11. Autenticação e segurança

### Login

Login utiliza e-mail e senha.

A senha é verificada por BCrypt.

Quando o usuário não existe, é realizada comparação
com um hash auxiliar para reduzir diferenças
de processamento.

Esse recurso não garante tempo constante.

### Access token e sessão

A estratégia utiliza:

- JWT com validade máxima de 15 minutos.
- Sessão com validade absoluta de oito horas.
- Expiração do JWT limitada pela expiração da sessão.
- Identificação de usuário e sessão nas claims.

A validação considera:

- Assinatura HS256.
- Emissor.
- Audiência.
- Expiração.
- Sessão ativa.
- Correspondência entre usuário e sessão.

Senha, hash da senha e refresh token
não são incluídos nas claims.

### Refresh token

O refresh token utiliza 32 bytes aleatórios,
codificados em Base64 URL sem padding.

É enviado por cookie HttpOnly.

Somente o hash SHA-256 é persistido.

A renovação substitui o token sem estender
a validade absoluta da sessão.

A rotação verifica versão, hash anterior
e estado ativo em uma única atualização.

### Logout

O logout revoga a sessão e remove o cookie.

A consulta da sessão nos acessos protegidos
permite invalidar os JWTs associados à sessão revogada.

Após rotação, um cookie antigo deixa de localizar
a sessão pelo hash atual.

Foi acrescentada identificação por JWT válido
para permitir revogação com cookie desatualizado.

Esse fluxo verifica o proprietário da sessão.

Logout somente com cookie antigo continua
sendo uma limitação.

### Autenticação no frontend

O access token permanece em memória.

Não é persistido em localStorage.

Após recarregar a página, o frontend utiliza
o refresh cookie para restaurar a autenticação.

Context disponibiliza o estado e as operações
aos componentes.

A implementação centraliza a renovação,
compartilha sua execução entre chamadas
e serializa operações de autenticação.

A coordenação entre múltiplas abas
não possui validação específica.

### CSRF e cookies

Login, refresh e logout exigem token CSRF.

Ele é obtido por:

```http
GET /api/v1/auth/csrf
```

O frontend utiliza o token e o nome do cabeçalho
retornados pela API.

O refresh cookie utiliza:

- HttpOnly.
- SameSite=Lax.
- Path=/api/v1/auth.
- Secure configurável.

No HTTP local, Secure é desabilitado.

Em HTTPS, AUTH_COOKIE_SECURE deve ser true.

### Chave JWT

A chave é recebida por JWT_SECRET_BASE64.

Após decodificação, deve possuir pelo menos 32 bytes.

A chave local não deve ser versionada.

O perfil de testes utiliza uma chave conhecida,
exclusiva dos testes.

## 12. Regras das solicitações

O solicitante é obtido exclusivamente
da autenticação.

O cliente não escolhe o proprietário.

Categorias permitidas:

- TI.
- RH.
- COMPRAS.
- FINANCEIRO.
- INFRAESTRUTURA.

Regras de estado:

- Status inicial ABERTO.
- Edição somente em ABERTO.
- Exclusão somente em ABERTO.
- Exclusão física.
- ABERTO → EM_ATENDIMENTO → CONCLUIDO.
- Saltos, retornos e repetição de status são proibidos.

A edição preserva identidade, código,
proprietário e data de criação.

A alteração de status preserva
os dados descritivos.

As alterações atualizam updatedAt.

### Propriedade

Consulta, listagem, filtros, alterações
e dashboard são limitados ao usuário autenticado.

A consulta individual utiliza identificador
e proprietário na mesma busca.

Solicitações alheias e inexistentes
recebem a mesma resposta 404.

Não é realizada consulta global
para revelar a existência de registro alheio.

## 13. Controle de concorrência

Edição, exclusão e alteração de status
utilizam condições de:

- Identificador.
- Proprietário.
- Versão esperada.
- Status esperado.

Atualizações incrementam a versão
na mesma instrução.

Zero linhas afetadas após a leitura
resulta em conflito.

O mecanismo protege o intervalo entre leitura
e escrita no backend.

Não detecta especificamente um formulário antigo
no navegador, pois a versão não integra
o contrato do cliente.

As operações não utilizam atualização
genérica por save.

Os testes utilizam expectativas desatualizadas
de forma sequencial.

Não simulam múltiplas threads concorrentes.

## 14. Consultas, filtros e dashboard

### Listagem

A listagem utiliza paginação.

Parâmetros:

- page: iniciado em zero; padrão 0.
- size: de 1 a 100; padrão 20.
- status: opcional.
- categoria: opcional.
- titulo: opcional.
- dataInicial: opcional.
- dataFinal: opcional.

Os filtros são combinados com AND.

A condição de proprietário é obrigatória.

Ordenação:

- createdAt DESC.
- codigo DESC.

O código funciona como desempate
para datas iguais.

A resposta contém:

- content.
- page.
- size.
- totalElements.
- totalPages.

Conteúdo e totais respeitam
proprietário e filtros.

Página fora do intervalo retorna
conteúdo vazio.

### Título

A busca é parcial e não diferencia
maiúsculas e minúsculas.

Percentual e sublinhado são tratados
como conteúdo literal.

Não há remoção explícita de acentos.

### Período

As datas são interpretadas em America/Sao_Paulo.

A data inicial é inclusiva.

A data final é inclusiva para o usuário,
representada na consulta pelo início exclusivo
do dia seguinte.

Período invertido ou data inválida
retorna 400.

### Dashboard

Uma consulta agregada por proprietário retorna:

- total.
- abertas.
- emAtendimento.
- concluidas.

Os indicadores não dependem
dos filtros da listagem.

Não é necessário carregar todas
as solicitações em memória.

## 15. Contrato HTTP

A API utiliza o prefixo /api/v1.

| Método | Caminho | Sucesso |
|---|---|---|
| POST | /usuarios | 201 |
| GET | /usuarios/me | 200 |
| GET | /auth/csrf | 200 |
| POST | /auth/login | 200 |
| POST | /auth/refresh | 200 |
| POST | /auth/logout | 204 |
| POST | /solicitacoes | 201 |
| GET | /solicitacoes | 200 |
| GET | /solicitacoes/{id} | 200 |
| PUT | /solicitacoes/{id} | 204 |
| DELETE | /solicitacoes/{id} | 204 |
| PATCH | /solicitacoes/{id}/status | 204 |
| GET | /dashboard | 200 |

| Código HTTP | Uso |
|---|---|
| 400 | Entrada ou parâmetro inválido |
| 401 | Autenticação ausente ou inválida |
| 403 | Acesso negado ou CSRF inválido |
| 404 | Solicitação não encontrada para o proprietário |
| 409 | E-mail duplicado ou conflito de estado |
| 500 | Falha inesperada |

O ApiExceptionHandler padroniza os erros
da apresentação.

Erros inesperados não expõem
detalhes internos ao cliente.

A camada de segurança utiliza
um formato de erro reduzido.

A unificação desses formatos
é uma melhoria possível.

Documentação disponível:

- /v3/api-docs.
- /swagger-ui/index.html.

A documentação pública não libera
os endpoints protegidos.

## 16. Interface e responsividade

A interface contempla:

- Cadastro e login.
- Navegação principal.
- Dashboard.
- Listagem e filtros.
- Criação e edição.
- Detalhes.
- Alteração de status.
- Exclusão.
- Mensagens de carregamento, validação e falha.

Os controles de edição e exclusão
são apresentados conforme o status.

O backend verifica as regras
independentemente da interface.

Os formulários utilizam rótulos,
mensagens por campo e elementos semânticos.

### Responsividade

Os estilos incluem ajustes para:

- Navegação.
- Indicadores.
- Filtros.
- Formulários.
- Detalhes.
- Tabela com rolagem horizontal interna.

A revisão visual manual em desktop
e largura de 390 px foi concluída
com sucesso pelo desenvolvedor,
sem problemas identificados.

O cenário integrado também foi aprovado
em emulação móvel com Chromium.

Não foram realizados testes
em dispositivos físicos nem uma auditoria
completa de acessibilidade.

## 17. Comunicação e execução integrada

O frontend utiliza /api
na mesma origem do navegador.

No desenvolvimento, o Vite
encaminha as chamadas ao backend.

No Docker, o Nginx realiza
esse encaminhamento.

Os proxies preservam os caminhos
da API e os cookies.

Esse arranjo evita a necessidade
de CORS no fluxo adotado.

### Backend em Docker

O Dockerfile utiliza:

- JDK 21 para compilação.
- JRE 21 para execução.
- Usuário sem privilégios administrativos.

O Compose inicia:

- Backend.
- PostgreSQL.

O banco possui volume persistente
e healthcheck.

O backend aguarda a disponibilidade
do banco.

### Frontend em Docker

O Dockerfile utiliza:

- Node.js para instalar dependências e compilar.
- Nginx para servir os arquivos estáticos.

O Nginx também utiliza execução
sem privilégios administrativos.

O fallback para index.html permite
acesso direto às rotas React.

### Rede compartilhada

O Compose do frontend utiliza
a rede externa criada pelo backend.

O nome pode ser configurado
por BACKEND_NETWORK.

Por isso, o backend deve ser iniciado
antes do frontend.

### Portas locais

Configuração padrão:

- Backend: 8080.
- Frontend: 3000.
- PostgreSQL: 5434.

O frontend acessa o backend
pela rede interna Docker.

### Dados

docker compose down encerra os serviços
preservando o volume do PostgreSQL.

A remoção explícita do volume
elimina os dados locais.

Os testes backend utilizam
PostgreSQL temporário separado.

Os testes E2E utilizam o ambiente integrado
e criam dados nesse banco.

As instruções completas estão
nos READMEs dos repositórios.

## 18. Testes e evidências

### Backend

Testes unitários verificam
domínio e casos de uso.

Testes de integração verificam HTTP
e persistência com PostgreSQL real.

Cobertura:

- Cadastro e validações.
- Duplicidade de e-mail.
- Hash de senha.
- Login, renovação e logout.
- Expiração e revogação.
- Provedores de tokens.
- Criação e consulta.
- Isolamento por proprietário.
- Paginação, ordenação e filtros.
- Período e busca literal.
- Edição e exclusão.
- Transições de status.
- Condições de escrita.
- Dashboard.
- Logout com cookie anterior à rotação.

### Frontend

A suíte Vitest contempla:

- Validação de formulários.
- Normalização das entradas.
- Casos de uso de solicitações.
- Restrições de edição e exclusão.
- Avanço de status.

### E2E

O cenário integrado verifica:

- Cadastro.
- Login.
- Restauração após recarregar.
- Criação.
- Edição.
- Listagem e filtros.
- Detalhes.
- Alteração de status.
- Restrição dos controles.
- Exclusão física.
- Dashboard.
- Logout.
- Permanência no login após recarregar.

O teste utiliza e-mail único
para cada execução.

As esperas utilizam rotas,
elementos e respostas HTTP.

Não foram utilizadas esperas fixas
para esconder falhas de sincronização.

### Resultados informados

| Verificação | Resultado |
|---|---|
| Backend: Maven verify | 153 testes aprovados |
| Backend: falhas | Zero |
| Backend: erros | Zero |
| Backend: ignorados | Zero |
| Listagem HTTP | 18 execuções aprovadas |
| Frontend: Vitest | 5 testes aprovados |
| E2E desktop | 3 execuções consecutivas aprovadas |
| E2E móvel | 1 execução aprovada |
| Revisão visual desktop e 390 px | Concluída com sucesso |
| Backend pelo Compose | Inicialização confirmada |
| Saúde | HTTP 200, status UP |
| Frontend pelo Nginx | HTTP 200 |
| CSRF pelo Nginx | HTTP 200 |
| Swagger UI | HTTP 200 |
| Dashboard sem autenticação | HTTP 401 |

Os resultados foram apresentados
pelo desenvolvedor a partir do Ubuntu/WSL.

Desktop e emulação móvel utilizam
o mesmo cenário E2E.

As repetições não representam
cenários independentes adicionais.

Os testes não garantem ausência de defeitos.

## 19. Integração contínua

### Backend

O GitHub Actions executa:

- Maven verify.
- Build Docker.

O build da imagem utiliza -DskipTests,
pois os testes são executados
na verificação Maven.

### Frontend

O GitHub Actions executa:

- npm ci.
- ESLint.
- Vitest.
- Build Vite.
- Build Docker.

### Limitações

Os testes E2E são executados localmente.

Ainda não integram a CI, pois dependem
da inicialização dos dois repositórios
e do banco.

Não há publicação de imagens
nem deploy automático.

### Falhas de infraestrutura observadas

Execuções finais encontraram falha
na disponibilização dos runners hospedados.

A mensagem apresentada foi:

    The job was not acquired by Runner of type hosted even after multiple attempts

Essa falha não comprova problema
nos testes ou no Dockerfile.

O job afetado não chegou
a executar sua verificação.

Os PRs foram concluídos conforme
informado pelo desenvolvedor.

A aprovação de PR não substitui
a confirmação dos jobs da CI.

Os resultados finais devem ser conferidos
nas abas Actions dos repositórios.

## 20. Problemas encontrados e ajustes

### Chave JWT

A variável havia sido direcionada
ao PostgreSQL no Compose.

Foi corrigida para ser fornecida
ao backend.

### Perfil de testes

A configuração de teste e a ativação
do perfil test foram acrescentadas
às classes de integração.

Isso permite utilizar uma chave
exclusiva dos testes.

### CSRF

Renovação e logout receberam 403
nos testes por ausência ou uso
incorreto do token CSRF.

Os helpers foram corrigidos
para obter e enviar o token
com os cookies correspondentes.

A proteção permaneceu habilitada.

### Logout após rotação

Cookie antigo não localizava
a sessão pelo hash atual.

Foi acrescentado encerramento
por JWT válido e teste de regressão.

### Conferência dos requisitos

Filtros por período e título
foram identificados como ausentes.

A implementação e os testes
da listagem foram ampliados.

### Configuração inicial frontend

Scripts e dependências necessários
estavam ausentes na configuração inicial.

Foram corrigidos os scripts de lint
e build e a dependência do plugin React.

Lint, testes e build passaram
após as correções.

### Proxy Nginx

A configuração inicial retornava 503
nas chamadas /api.

Foi implementado o encaminhamento
ao backend pela rede compartilhada.

A consulta de CSRF pelo frontend
confirmou o proxy.

### Arquivos gerados no Git

dist e node_modules foram identificados
no versionamento.

Foram removidos do índice
e incluídos nas regras de exclusão.

package.json e package-lock.json
permanecem versionados.

### Descoberta dos testes E2E

O Playwright inicialmente
não encontrava o cenário.

A configuração de descoberta
e o caminho do arquivo foram corrigidos.

### Sincronização do login no E2E

O teste tentava preencher o login
antes da conclusão da navegação
a partir do cadastro.

Foram acrescentadas esperas
pela rota e pelos elementos corretos.

O cenário passou em três execuções
consecutivas desktop após a correção.

## 21. Uso de LLM e participação do desenvolvedor

ChatGPT apoiou:

- Comparação de tecnologias.
- Propostas de arquitetura.
- Elaboração de código.
- Elaboração de testes.
- Diagnóstico de falhas.
- Atualização documental.
- Redação de commits e PRs.

O desenvolvedor definiu o escopo,
solicitou ajustes, revisou propostas
e executou as verificações.

Decisões e ajustes solicitados:

- Camadas na raiz do pacote.
- Classes explícitas para entidades.
- Nome mínimo de três caracteres.
- Model na persistência.
- Mappers específicos.
- Repositórios separados.
- Context para autenticação.
- Axios para comunicação.
- Zod para formulários.
- Docker e CI desde o início.
- Documentação por etapa.
- Arquivos completos.
- Testes complementares.

Os registros estão em docs/llm
nos dois repositórios.

A assistência não substitui
a responsabilidade do desenvolvedor
pelo código e pela explicação
das decisões.

## 22. Análise crítica e evolução

### Limitações atuais

- Sem papéis administrativos.
- Sem recuperação de senha.
- Sem verificação de e-mail.
- Sem auditoria histórica.
- Exclusão física sem recuperação.
- Sem limpeza automática de sessões.
- Sessão consultada no banco
  em cada acesso protegido.
- Logout somente com cookie antigo
  pode não localizar a sessão.
- Coordenação entre múltiplas abas
  sem testes específicos.
- Busca textual sem remoção de acentos.
- Paginação por offset.
- Sem versão ou ETag no contrato
  para detectar formulário antigo.
- Concorrência verificada
  por cenários sequenciais.
- Formatos distintos de erro
  entre segurança e controllers.
- Schemas de erro do OpenAPI
  ainda não detalhados.
- Suíte frontend menor
  que a suíte backend.
- E2E com um cenário amplo,
  sem todos os fluxos negativos.
- E2E fora da CI.
- Sem testes de carga.
- Sem testes em dispositivos físicos.
- Sem auditoria completa de acessibilidade.
- Sem publicação ou deploy automático.
- Imagens sem fixação de todos os digests.

### Melhorias futuras

- Padronizar os erros HTTP.
- Avaliar TypeScript no frontend.
- Ampliar testes frontend
  e cenários E2E negativos.
- Executar E2E na CI.
- Avaliar versão explícita
  ou ETag no contrato.
- Implementar limpeza de sessões.
- Avaliar índices conforme
  volume e plano de execução.
- Adicionar concorrência real
  aos testes.
- Ampliar observabilidade.
- Implementar auditoria
  conforme necessidade.
- Avaliar papéis de atendimento
  conforme novos requisitos.

### Avaliações para produção

- HTTPS e cookies Secure.
- Gestão e rotação de segredos.
- Proteção contra abuso
  no cadastro e login.
- Recuperação de senha
  e verificação de e-mail.
- Backup e teste de restauração.
- Monitoramento e alertas.
- Logs estruturados
  sem credenciais.
- Retenção de dados.
- Testes de carga.
- Revisão de acessibilidade.
- Atualização controlada
  de dependências e imagens.
- Processo de deploy
  com ambientes e verificações.
- Possível provedor de identidade.

Essas decisões devem considerar
volume, risco e requisitos operacionais.

Não há necessidade demonstrada
de microsserviços para este escopo.

## 23. Demonstração e estado de conclusão

### Demonstração

As instruções de configuração,
execução e testes estão nos READMEs.

O avaliador pode criar uma conta
pela interface e executar os fluxos.

Usuários de demonstração
não são inseridos automaticamente
pelas migrations.

As credenciais indicadas no README
dependem da criação prévia da conta.

### Estado de conclusão

Backend e frontend possuem
os fluxos funcionais implementados
e validação local apresentada.

A integração com Docker e Nginx
foi confirmada.

Resultados informados:

- Backend: 153 testes aprovados.
- Frontend: cinco testes Vitest aprovados.
- E2E desktop: três execuções
  consecutivas aprovadas.
- E2E com emulação móvel:
  uma execução aprovada.
- Revisão visual em desktop
  e largura de 390 px:
  concluída com sucesso.

Os PRs das etapas foram concluídos.

A aprovação dos PRs não substitui
a confirmação dos resultados da CI.

### Conferências de fechamento concluídas

- Memorial atualizado e disponível na main do backend.
- READMEs dos dois repositórios conferidos.
- Resultados finais da CI conferidos e aprovados.
- Links e instruções de entrega conferidos.

As conferências foram concluídas pelo desenvolvedor
em 5 de outubro de 2026.