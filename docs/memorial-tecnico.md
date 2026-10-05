# Memorial Técnico de Desenvolvimento

## 1. Identificação

Projeto: Portal de Solicitações Internas.

Autor: Wenned Chaves.

Finalidade: mini-projeto Full Stack para o processo seletivo
de Desenvolvedor de Sistemas Júnior da bit Soluções.

Data desta versão: 5 de outubro de 2026.

Repositórios:

- Backend: https://github.com/JoseWenned/portal-solicitacoes-backend
- Frontend: https://github.com/JoseWenned/portal-solicitacoes-frontend

Estado do documento: versão parcial.

O backend está implementado e validado localmente.
Frontend, integração pelo navegador e revisão final permanecem pendentes.

Este memorial registra decisões, justificativas, processo de desenvolvimento,
evidências e limitações. Será atualizado durante as etapas restantes.

## 2. Interpretação do problema

O portal permite que colaboradores registrem demandas internas,
acompanhem sua evolução e consultem indicadores.

O enunciado exige:

- Autenticação, controle de sessão e logout.
- Criação, consulta e gerenciamento de solicitações.
- Edição e exclusão de solicitações abertas.
- Filtros por período, categoria, status e título.
- Dashboard com total e contagens por status.
- Backend, frontend e persistência SQL.
- Instruções completas de execução.
- Scripts de criação da estrutura e dicionário de dados.
- README e Memorial Técnico de Desenvolvimento.

Docker, testes automatizados, CI/CD e responsividade são diferenciais.

Docker, testes e CI foram incluídos desde o início.
Responsividade será implementada e verificada no frontend.

A CI atual não realiza deploy ou publicação de imagens.

## 3. Decisões de escopo

As decisões adotadas durante a análise foram:

- Cadastro de usuário com name, email e password.
- Login por e-mail e senha.
- Usuários de demonstração criados pelo cadastro.
- Acesso somente às próprias solicitações.
- Solicitante obtido exclusivamente da autenticação.
- Status inicial ABERTO.
- Fluxo ABERTO → EM_ATENDIMENTO → CONCLUIDO.
- Edição e exclusão somente em ABERTO.
- Exclusão física.
- Dashboard restrito ao usuário autenticado.

O e-mail concretiza o campo usuário previsto para o login.

O isolamento por proprietário é uma decisão de escopo adotada.
Não foram implementados papéis administrativos ou atendimento
por outro usuário.

## 4. Tecnologias implementadas e justificativas

As justificativas abaixo descrevem a escolha para este projeto,
sem representar uma superioridade universal sobre as alternativas.

### Java 21

Motivo: linguagem estudada pelo desenvolvedor, com tipagem estática
e recursos adequados às regras e contratos do backend.

Benefícios: verificação de tipos e ferramentas consolidadas
de desenvolvimento e testes.

Alternativa: JavaScript no backend compartilharia a linguagem
com o frontend. Java foi escolhido pela familiaridade com Spring.

Impacto: favorece manutenção de contratos explícitos,
com necessidade de uma JVM e maior estrutura inicial.

### Spring Boot 4.1.1 e Spring Web MVC

Motivo: criar uma API HTTP com configuração integrada.

Benefícios: integração entre injeção de dependências, HTTP,
persistência, validação, segurança e testes.

Alternativas: configuração manual do Spring ou frameworks menores
exigiriam outras decisões de integração.

Impacto: reduz configuração inicial, mas exige conhecimento
das convenções e compatibilidade das dependências.

### Maven e Maven Wrapper

Motivo: padronizar dependências, build e comandos.

Benefícios: o Wrapper utiliza a versão definida no projeto
sem instalação global do Maven.

Alternativa: Gradle também atenderia ao projeto.
Maven foi escolhido pela familiaridade e configuração declarativa.

Impacto: favorece reprodução local e na CI.
A primeira execução exige download das dependências.

Versão configurada: Maven 3.9.16.

### PostgreSQL 16

Motivo: persistir dados relacionais com integridade referencial.

Benefícios: transações, restrições, UUID, TIMESTAMPTZ
e consultas agregadas.

Alternativas: MySQL atenderia ao cenário; banco em memória
ofereceria menor fidelidade ao ambiente utilizado.

Impacto: protege integridade e executa filtros no banco.
Produção exigiria backup, monitoramento e planejamento de capacidade.

### Spring Data JPA e Hibernate

Motivo: integrar persistência, consultas e paginação ao Spring.

Benefícios: repositórios, mapeamento e composição de filtros
com Specification.

Alternativa: JDBC em todas as operações daria controle direto
do SQL, com mais mapeamento manual.

Impacto: reduz repetição, mas exige atenção às transações,
consultas geradas e contexto de persistência.

Os modelos JPA são separados das entidades de domínio.
Open-in-view está desativado.

### JdbcTemplate

Motivo: calcular o dashboard em uma única consulta agregada.

Benefícios: SQL explícito sem carregar solicitações em memória.

Alternativa: consultas agregadas por JPA também seriam possíveis.

Impacto: simplifica essa operação específica, mantendo a dependência
do SQL na infraestrutura.

### Flyway

Motivo: versionar a estrutura do banco junto do código.

Benefícios: execução ordenada e histórico de migrations.

Alternativa: geração pelo Hibernate facilitaria o início,
mas deixaria a evolução da estrutura menos explícita.

Impacto: favorece reprodução e revisão das mudanças.
Migrations aplicadas são preservadas.

Hibernate utiliza ddl-auto=validate.

### Bean Validation

Motivo: validar dados recebidos pelos controllers.

Benefícios: validação declarativa e identificação dos campos inválidos.

Alternativa: validação exclusivamente manual exigiria mais repetição.

Impacto: melhora consistência das entradas, sem substituir
as regras de domínio ou restrições do banco.

### Spring Security e OAuth2 Resource Server

Motivo: integrar autenticação HTTP e validação de Bearer JWT.

Benefícios: cadeia de filtros, assinatura, autorização e CSRF.

Alternativa: filtros próprios aumentariam a responsabilidade
de implementar corretamente o protocolo.

Impacto: centraliza a segurança HTTP. As regras adicionais
de sessão e propriedade continuam sendo responsabilidade da aplicação.

### JWT e sessões revogáveis

Motivo: utilizar Bearer tokens e permitir revogação imediata
por estado persistido.

Benefícios: identificação de usuário e sessão e access token curto.

Alternativa: sessão tradicional por cookie também atenderia
ao portal e teria um fluxo mais simples.

Impacto: exige coordenação entre tokens, cookies, rotação e CSRF.
Como a sessão é consultada no banco, a solução mantém estado.

JWT foi uma preferência do desenvolvedor.
Seu uso isolado não determina maior qualidade da solução.

### BCrypt

Motivo: proteger senhas com hash específico para esse propósito.

Benefícios: salt e custo configurável.

Alternativa: Argon2 poderia ser avaliado em outro contexto.

Impacto: custo 12 aumenta o trabalho de verificação.
A senha é limitada a 72 bytes em UTF-8.

### SecureRandom e SHA-256

Motivo: gerar refresh tokens aleatórios e persistir somente seu hash.

Benefícios: geração de 32 bytes aleatórios e armazenamento
da representação SHA-256 hexadecimal.

Alternativa: guardar o token original exporia diretamente
essa credencial em um acesso ao banco.

Impacto: permite consulta pelo hash e rotação.
SHA-256 é utilizado para tokens aleatórios, não para senhas.

### springdoc-openapi 3.1.1 e Swagger UI

Motivo: disponibilizar contrato HTTP e exploração interativa.

Benefícios: facilita consulta aos endpoints e verificação manual.

Alternativa: documentação manual isolada exigiria mais esforço
para acompanhar mudanças.

Impacto: melhora comunicação do contrato, mas exige revisão
das respostas e das regras de segurança.

Contrato gerado observado: OpenAPI 3.1.0.

### Actuator

Motivo: fornecer verificação de saúde.

Benefícios: endpoint padronizado para conferir disponibilidade.

Alternativa: endpoint próprio exigiria implementação adicional.

Impacto: auxilia execução e diagnóstico.
Somente saúde é exposta, sem detalhes internos.

Status UP não comprova todos os fluxos de negócio.

### JUnit e Mockito

Motivo: verificar entidades e casos de uso.

Benefícios: cenários repetíveis e dependências controladas.

Alternativa: testes somente manuais seriam menos reproduzíveis.

Impacto: favorecem manutenção das regras.
Mocks são complementados por integração real.

### Spring Boot Test e Testcontainers

Motivo: testar contexto, HTTP e persistência com PostgreSQL real.

Benefícios: banco temporário separado do Compose e maior fidelidade.

Alternativa: H2 poderia divergir nos tipos, consultas e restrições.

Impacto: aumenta confiança na integração, com maior tempo
de execução e dependência do Docker.

### JsonPath e AssertJ

Motivo: consultar respostas JSON e escrever verificações legíveis.

Benefícios: acesso aos campos do contrato e mensagens de falha claras.

Alternativa: leitura manual do JSON e assertions básicas
exigiriam mais código de apoio.

Impacto: simplifica manutenção dos testes HTTP.

### Docker e Docker Compose

Motivo: executar aplicação e banco com configuração explícita.

Benefícios: isolamento e reprodução do ambiente.

Alternativa: instalação manual exigiria mais configuração do avaliador.

Impacto: facilita execução, com dependência do daemon Docker
e download das imagens.

O Dockerfile compila com JDK 21 e executa com JRE 21.
A aplicação utiliza usuário sem privilégios administrativos.

O Compose utiliza volume persistente e healthcheck do PostgreSQL.

### Git, GitHub e GitHub Actions

Motivo: versionar o desenvolvimento e automatizar verificações.

Benefícios: branches por etapa, PRs revisáveis e CI.

Alternativas: outros serviços Git e pipelines atenderiam ao projeto.

Impacto: melhora rastreabilidade.
Não há publicação de imagens ou deploy automatizado.

### Ubuntu no WSL

Motivo: desenvolver com ferramentas Linux na máquina Windows.

Benefícios: terminal e integração com Java, Git e Docker.

Alternativa: desenvolvimento diretamente no Windows seria possível.

Impacto: mantém o ambiente familiar ao desenvolvedor.
Containers reduzem a dependência desse ambiente para o avaliador.

### Python 3

Motivo: gerar a chave JWT local com um script pequeno.

Benefícios: bibliotecas padrão e geração sem imprimir o segredo.

Alternativa: outras ferramentas de geração aleatória seriam possíveis.

Impacto: ferramenta auxiliar de configuração.
Não participa da execução da API.

### ChatGPT

Motivo: apoiar decisões, propostas de código, diagnóstico e documentação
durante o desenvolvimento.

Benefícios: acelera a elaboração de alternativas e a revisão textual.

Alternativas: documentação oficial, pesquisa e implementação
sem assistência continuam sendo recursos disponíveis.

Impacto: exige revisão humana, execução dos testes e conferência
das propostas contra os requisitos. Não é dependência da aplicação.

## 5. Frontend planejado

Tecnologias aprovadas:

- JavaScript e React.
- Vite.
- SCSS.
- React Router.
- Axios.
- React Hook Form e Zod.
- Context.
- Vitest e React Testing Library.
- Playwright.
- Docker e Nginx.
- GitHub Actions.

Ainda não foram implementadas nesta versão.

As justificativas, versões e evidências serão acrescentadas durante
o desenvolvimento. Planejamento não será apresentado como implementação.

## 6. Arquitetura

O backend é organizado por camadas na raiz do pacote.

| Camada | Responsabilidade |
|---|---|
| domain | Entidades, invariantes e exceções de negócio |
| application | Casos de uso, contratos e resultados |
| infrastructure | Persistência, segurança, mappers e composição |
| presentation | HTTP, DTOs e tratamento de erros |

Dentro das camadas, os arquivos são agrupados por responsabilidade
e conceito de negócio.

Domínio e aplicação não dependem de Spring ou JPA.
As portas descrevem dependências dos casos de uso.
Adaptadores implementam as portas.
Configurações Spring conectam as implementações.

Modelos de persistência utilizam a nomenclatura Model.
Mappers explícitos realizam conversões com o domínio.

O resultado de paginação da aplicação não utiliza tipos do Spring Data.

### Clean Architecture e SOLID

A separação busca manter as regras independentes de infraestrutura.

Exemplos:

- Responsabilidade única entre controllers, casos de uso e adaptadores.
- Inversão de dependências por contratos.
- Interfaces organizadas por responsabilidade.
- Composição das implementações fora do domínio.

Não foram criadas abstrações apenas para demonstrar cada princípio.

### Conceitos de DDD

- Identidade por UUID.
- Vocabulário de negócio explícito.
- Regras próximas às entidades.
- Criação e reconstituição controladas.
- Preservação das invariantes.

Esses conceitos não representam implementação de todos
os padrões estratégicos e táticos de DDD.

Microsserviços e infraestrutura distribuída adicional
não foram considerados necessários para o escopo.

## 7. Modelagem e persistência

Tabelas de negócio:

- usuarios
- solicitacoes
- sessoes_autenticacao

Um usuário pode possuir várias solicitações e sessões.

UUID identifica tecnicamente os registros.
Solicitações também possuem código numérico gerado pelo PostgreSQL.

Restrições únicas protegem e-mail, código e hash atual do refresh token.
Chaves estrangeiras exigem usuários existentes.

[Dicionário de dados](database/dicionario-dados.md)

Migrations V1, V2 e V3 criam a estrutura.
Flyway aplica as mudanças; Hibernate valida o mapeamento.

### Controle das escritas

Edição, exclusão e alteração de status utilizam condições
de identificador, proprietário, versão e status.

Atualizações incrementam a versão na mesma instrução.
Zero linhas afetadas após a leitura resulta em conflito.

O mecanismo protege o intervalo entre leitura e escrita no backend.
Não detecta especificamente formulário antigo no navegador,
pois a versão não faz parte do contrato do cliente.

## 8. Autenticação e segurança

Cadastro recebe name, email e password.
Não autentica automaticamente.

Login utiliza e-mail e senha, verificada por BCrypt.

Quando o usuário não existe, é realizada comparação com hash auxiliar
para reduzir diferenças de processamento.
Isso não garante tempo constante.

### Tokens e sessão

- JWT com validade máxima de 15 minutos.
- Refresh token aleatório em cookie HttpOnly.
- Sessão com validade absoluta de oito horas.
- Hash do refresh token armazenado no banco.
- Rotação sem extensão da expiração.
- Logout com revogação persistida.

A validação do JWT considera assinatura, emissor, audiência,
validade e sessão ativa pertencente ao usuário.

Senha, hash de senha e refresh token não são incluídos nas claims.

### CSRF e cookies

Login, refresh e logout exigem token CSRF,
obtido por GET /api/v1/auth/csrf.

Refresh cookie utiliza HttpOnly, SameSite=Lax
e Path=/api/v1/auth.

Secure é configurável para HTTP local e deve ser habilitado em HTTPS.

### Revisão do logout

Após rotação, o hash anterior deixa de identificar a sessão.
Foi acrescentada identificação por JWT válido para permitir revogação
mesmo com cookie desatualizado.

A identificação por JWT verifica o proprietário.
O fluxo por cookie permanece disponível.

Logout somente com cookie antigo continua sendo uma limitação.
O frontend deverá coordenar renovação e logout
e descartar respostas atrasadas.

## 9. API e regras de negócio

A API utiliza o prefixo /api/v1.

Inclui cadastro, autenticação, consulta do usuário,
operações de solicitações e dashboard.

| Código HTTP | Uso |
|---|---|
| 200 | Consultas, login e renovação |
| 201 | Cadastro e criação |
| 204 | Edição, exclusão, alteração de status e logout |
| 400 | Entrada inválida |
| 401 | Autenticação ausente ou inválida |
| 403 | Acesso negado ou CSRF inválido |
| 404 | Solicitação não encontrada para o proprietário |
| 409 | E-mail duplicado ou conflito de estado |
| 500 | Falha inesperada |

Solicitações alheias e inexistentes recebem a mesma resposta 404.

### Solicitações

- Status inicial ABERTO.
- Edição e exclusão somente em ABERTO.
- Exclusão física.
- Fluxo ABERTO → EM_ATENDIMENTO → CONCLUIDO.
- Propriedade obtida da autenticação.
- Campos automáticos controlados pelo backend.

### Filtros

A listagem possui paginação e filtros por status, categoria,
texto parcial no título e período de criação.

Todos são combinados por AND e limitados ao proprietário.

Datas são interpretadas no fuso America/Sao_Paulo.
O final inclusivo utiliza o início exclusivo do dia seguinte.

Título não diferencia maiúsculas e minúsculas.
Percentual e sublinhado são literais.
Não há remoção explícita de acentos.

### Dashboard

Uma consulta agregada por proprietário retorna total,
abertas, emAtendimento e concluidas.

Os indicadores não dependem dos filtros da listagem.

## 10. Comunicação planejada com o frontend

O frontend utilizará /api na mesma origem do navegador,
com proxy do Vite no desenvolvimento e Nginx no Docker.

Os proxies deverão preservar /api/v1 e os cookies.

Access token será mantido em memória.
Restauração após recarregar utilizará refresh cookie e CSRF.

Integração e coordenação das chamadas ainda não foram validadas.

## 11. Processo de desenvolvimento

Etapas realizadas:

1. Análise e decisões de escopo.
2. Definição da stack e arquitetura.
3. Estrutura inicial com Docker e CI.
4. Persistência e migrations.
5. Cadastro.
6. Autenticação e sessões.
7. Criação de solicitações.
8. Consulta e listagem.
9. Edição, exclusão e status.
10. Dashboard.
11. OpenAPI.
12. Revisão do logout.
13. Filtros por período e título.
14. Consolidação inicial da documentação.

Foram utilizadas branches e PRs por etapa.
Commits e descrições registram alterações e validações.

A documentação foi atualizada durante o desenvolvimento,
incluindo arquitetura e registros de apoio de LLM.

## 12. Problemas e ajustes

### Chave JWT

A variável havia sido direcionada ao PostgreSQL no Compose.
Foi corrigida para ser fornecida ao backend.

### Perfil de testes

A configuração de teste e a ativação do perfil test foram acrescentadas
às classes de integração para utilizar chave exclusiva dos testes.

### CSRF

Renovação e logout receberam HTTP 403 nos testes.
Os helpers foram corrigidos para obter e enviar o token CSRF
com os cookies correspondentes.

### Logout após rotação

Cookie antigo não localizava a sessão pelo hash atual.
Foi acrescentado encerramento por JWT e teste de regressão.

### Conferência do enunciado

Foram identificados filtros por período e título ainda ausentes.
A implementação e os testes da listagem foram ampliados.

Dicionário de dados e memorial foram incluídos na consolidação documental.

## 13. Testes e evidências

Testes unitários verificam domínio e casos de uso.
Testes de integração verificam HTTP e persistência com PostgreSQL real.

Testcontainers fornece banco temporário separado do Compose.

Cobertura:

- Cadastro, validações, duplicidade e hash.
- Login, renovação, logout, expiração e revogação.
- Provedores de tokens.
- Criação e consulta de solicitações.
- Isolamento por proprietário.
- Paginação, ordenação e filtros.
- Limites do período e busca literal.
- Edição, exclusão e transições.
- Condições de escrita.
- Dashboard.
- Logout com cookie anterior à rotação.

### Resultados informados

- Listagem HTTP: 18 execuções aprovadas.
- Suíte completa: 153 testes.
- Falhas: zero.
- Erros: zero.
- Testes ignorados: zero.
- Maven verify: BUILD SUCCESS.
- Backend executado com Docker Compose.
- Saúde observada com status UP.
- Swagger acessível e contrato OpenAPI conferido.

Resultados apresentados pelo desenvolvedor a partir do Ubuntu/WSL.

Os testes de expectativas desatualizadas são sequenciais.
Não simulam múltiplas threads.

A integração pelo navegador ainda não foi comprovada.

## 14. CI e execução

GitHub Actions executa verificação Maven e build Docker.

O build Docker utiliza -DskipTests porque os testes
são executados na verificação Maven.

Não há publicação de imagens ou deploy automatizado.

O resultado da CI da branch de filtros ainda precisa ser atualizado
nesta versão documental.

README contém configuração, variáveis, execução do backend,
endpoints e testes.

As instruções completas da aplicação integrada serão adicionadas
após implementação do frontend.

## 15. Uso de LLM e participação do desenvolvedor

ChatGPT apoiou decisões, propostas de código, testes,
diagnóstico, commits, PRs e documentação.

O desenvolvedor definiu escolhas de escopo e stack,
solicitou ajustes e executou as verificações.

Ajustes solicitados:

- Camadas na raiz do pacote.
- Classes explícitas para entidades.
- Nome mínimo de três caracteres.
- Model em vez de entity na persistência.
- Mappers específicos.
- Repositórios separados.
- Docker, CI e documentação desde o início.
- Arquivos completos e testes complementares.

Registros estão em docs/llm.

As propostas foram submetidas à revisão e execução.
O desenvolvedor mantém responsabilidade pelo código entregue
e pela explicação das decisões.

## 16. Análise crítica

### Limitações

- Frontend e integração ainda pendentes.
- Coordenação de renovação e logout ainda pendente.
- Logout somente com cookie antigo pode não localizar a sessão.
- Sem papéis administrativos.
- Sem auditoria histórica.
- Sem limpeza automática de sessões.
- Busca textual sem índice específico ou remoção de acentos.
- Sem controle de formulário antigo no contrato.
- Concorrência testada por cenários sequenciais.
- Diferenças entre erros da segurança e dos controllers.
- Schemas de erro do OpenAPI ainda não detalhados.
- Sem deploy ou publicação automatizados.

### Melhorias futuras

- Padronizar erros HTTP.
- Avaliar ETag ou versão explícita no contrato.
- Implementar retenção e limpeza de sessões.
- Avaliar índices conforme volume e plano de execução.
- Ampliar observabilidade.
- Automatizar backup e testar restauração.
- Adicionar testes de concorrência real.
- Avaliar papéis de atendimento conforme novos requisitos.

### Avaliações para produção

- Gestão e rotação de segredos.
- HTTPS e configuração dos cookies.
- Proteção contra abuso no cadastro e login.
- Monitoramento e alertas.
- Retenção e auditoria.
- Recuperação e disponibilidade do PostgreSQL.
- Controle das dependências e imagens.
- Processo de deploy com ambientes e verificações.
- Possível integração com provedor de identidade.

Essas decisões dependeriam dos requisitos operacionais.
Não há necessidade demonstrada de microsserviços neste escopo.

## 17. Etapas restantes

- Implementar frontend.
- Validar proxies e cookies.
- Coordenar autenticação e restauração.
- Verificar fluxos completos no navegador.
- Implementar e validar responsividade.
- Documentar demonstração e execução integrada.
- Completar justificativas das tecnologias frontend.
- Registrar resultados finais de CI.
- Revisar memorial e README contra o enunciado.

Esta versão será concluída com evidências efetivamente obtidas,
sem apresentar planejamento como implementação finalizada.