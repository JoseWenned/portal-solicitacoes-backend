# Portal de Solicitações Internas — Backend

API do mini-projeto Full Stack desenvolvido para o processo seletivo
de Desenvolvedor de Sistemas Júnior da bit Soluções.

## Objetivo

Permitir que usuários registrem e acompanhem suas próprias solicitações
internas, com autenticação, filtros e indicadores.

## Estado atual

Implementado e validado localmente:

- Base Spring Boot com Maven Wrapper.
- Endpoint de saúde.
- Conexão com PostgreSQL.
- Migrations Flyway para usuários, solicitações e sessões de autenticação.
- Cadastro de usuário com validação e e-mail único.
- Armazenamento de senha com hash BCrypt.
- Login por e-mail e senha.
- Access token JWT com validade máxima de 15 minutos.
- Refresh token aleatório em cookie HttpOnly.
- Sessões revogáveis com validade absoluta de oito horas.
- Rotação do refresh token sem extensão da validade da sessão.
- Logout com revogação por refresh cookie ou JWT válido.
- Logout com JWT válido mesmo após rotação do refresh cookie.
- Validação da sessão associada ao JWT nos acessos protegidos.
- Proteção CSRF nos endpoints de login, renovação e logout.
- Consulta do usuário autenticado.
- Criação de solicitações vinculadas ao usuário autenticado.
- Status inicial ABERTO e código numérico gerado pelo PostgreSQL.
- Consulta individual limitada ao proprietário.
- Listagem paginada limitada ao proprietário.
- Filtros por status e categoria.
- Edição de solicitações próprias somente em ABERTO.
- Exclusão física de solicitações próprias somente em ABERTO.
- Alteração de status no fluxo ABERTO → EM_ATENDIMENTO → CONCLUIDO.
- Escritas condicionadas ao proprietário, versão e status esperado.
- Dashboard com total e contagens por status do usuário.
- Contrato OpenAPI e interface Swagger.
- Tratamento de erros HTTP.
- Testes unitários e de integração com Testcontainers.
- Dockerfile e Docker Compose.
- Workflow de CI para verificação Maven e build Docker.

A suíte local possui 146 testes aprovados.

Ainda pendentes:

- Frontend e integração completa pelos proxies.
- Coordenação de renovação e logout no frontend.
- Testes de navegador do fluxo integrado.
- Consolidação do Memorial Técnico.

A CI da branch de revisão da integração permanece pendente.

## Tecnologias utilizadas

- Java 21 e Spring Boot 4.1.1.
- Maven Wrapper, configurado para Maven 3.9.16.
- Spring Web MVC, Bean Validation e Actuator.
- Spring Data JPA e Hibernate.
- JdbcTemplate para agregação do dashboard.
- PostgreSQL 16 e Flyway.
- Spring Security e OAuth2 Resource Server para validação JWT.
- BCrypt para proteção de senhas.
- springdoc-openapi 3.1.1 e Swagger UI.
- JUnit, Mockito e Testcontainers.
- Docker, Docker Compose e GitHub Actions.

## Organização do código

O backend é organizado por camadas:

- domain: entidades e regras de domínio.
- application: casos de uso, portas e resultados.
- infrastructure: persistência, mappers, segurança e configuração.
- presentation: controllers, DTOs e tratamento de erros HTTP.

Dentro das camadas, os arquivos são agrupados por responsabilidade
e conceito de negócio.

Domínio e aplicação permanecem independentes de Spring e JPA.
Os modelos de persistência são separados das entidades de domínio.
As conversões são realizadas por mappers específicos.

O contrato de paginação da aplicação não depende de Page ou Pageable
do Spring Data.

## Pré-requisitos

Para executar pelo Docker Compose:

- Docker com daemon acessível.
- Docker Compose.
- Python 3 para gerar a chave JWT pelo exemplo abaixo.

Para executar o Java diretamente no Ubuntu/WSL:

- JDK 21.
- PostgreSQL disponível, que pode ser iniciado pelo Compose.
- Acesso à internet na primeira execução do Maven Wrapper.

Não é necessária uma instalação global do Maven.

## Configuração

Na raiz do repositório, crie o arquivo local de configuração:

```bash
cp .env.example .env
```

Se o arquivo `.env` já existir, preserve-o e confira seus valores.

Configure uma senha local para o PostgreSQL.
O arquivo `.env` não deve ser versionado.

| Variável | Finalidade |
|---|---|
| BACKEND_PORT | Porta local do backend; padrão 8080 |
| POSTGRES_PORT | Porta local do banco; padrão 5434 |
| POSTGRES_DB | Nome do banco |
| POSTGRES_USER | Usuário do banco |
| POSTGRES_PASSWORD | Senha do banco |
| JWT_SECRET_BASE64 | Chave JWT em Base64, com pelo menos 32 bytes após decodificação |
| AUTH_COOKIE_SECURE | Cookies somente por HTTPS; false no ambiente HTTP local |

O Compose fornece ao backend `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
e a configuração de segurança.

O Spring Boot não carrega o arquivo `.env` automaticamente.

### Geração da chave JWT

Execute na raiz do repositório. O script preserva uma chave existente
e preenche a configuração quando estiver ausente ou vazia,
sem exibir o segredo:

```bash
python3 - <<'PY'
from pathlib import Path
import base64
import secrets

path = Path(".env")

if not path.is_file():
    raise SystemExit("Arquivo .env não encontrado na pasta atual.")

lines = path.read_text(encoding="utf-8").splitlines()
indexes = [
    index
    for index, line in enumerate(lines)
    if line.strip().startswith("JWT_SECRET_BASE64=")
]

if len(indexes) > 1:
    raise SystemExit("Há entradas duplicadas de JWT_SECRET_BASE64. Corrija o arquivo.")

if indexes and lines[indexes[0]].split("=", 1)[1].strip():
    print("JWT_SECRET_BASE64 já possui um valor. Chave preservada.")
else:
    value = base64.b64encode(secrets.token_bytes(32)).decode("ascii")
    entry = f"JWT_SECRET_BASE64={value}"

    if indexes:
        lines[indexes[0]] = entry
    else:
        lines.append(entry)

    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("Chave JWT configurada sem exibir seu valor.")
PY
```

Em ambientes HTTPS, configure `AUTH_COOKIE_SECURE=true`.

## Execução com Docker Compose

Confira a configuração e inicie os serviços:

```bash
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

O backend conecta-se a `database:5432` na rede interna.
O banco fica acessível localmente em `127.0.0.1:5434` por padrão.

O Flyway aplica as migrations na inicialização.
O Hibernate utiliza `ddl-auto=validate`.

Verifique a saúde:

```bash
curl --fail --retry 10 --retry-all-errors --retry-delay 2 \
  http://localhost:8080/actuator/health
```

Resultado esperado: `status` igual a `UP`.

Durante a inicialização, a conexão pode falhar temporariamente.
Se `BACKEND_PORT` foi alterada, ajuste a URL.

Para consultar logs:

```bash
docker compose logs --tail=100 backend database
```

Para encerrar preservando os dados:

```bash
docker compose down
```

## Execução Java no Ubuntu/WSL

Inicie somente o banco:

```bash
docker compose up -d database
```

Se o backend do Compose estiver em execução na mesma porta,
pare-o antes de iniciar o Java localmente:

```bash
docker compose stop backend
```

Exporte as variáveis usando os mesmos valores configurados no `.env`.
O exemplo considera banco e usuário com os nomes padrão:

```bash
export DB_URL='jdbc:postgresql://localhost:5434/portal_solicitacoes'
export DB_USERNAME='portal_app'

read -r -s -p 'Senha do banco: ' DB_PASSWORD
echo
export DB_PASSWORD

read -r -s -p 'Chave JWT em Base64: ' JWT_SECRET_BASE64
echo
export JWT_SECRET_BASE64

export AUTH_COOKIE_SECURE=false
```

Execute:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Se você alterou a porta, o nome do banco ou o usuário,
ajuste as variáveis correspondentes.

## Endpoints disponíveis

| Método | Endpoint | Finalidade | Proteção |
|---|---|---|---|
| GET | /actuator/health | Saúde da aplicação | Público |
| POST | /api/v1/usuarios | Cadastro de usuário | Público |
| GET | /api/v1/auth/csrf | Obtenção do token CSRF | Público |
| POST | /api/v1/auth/login | Login | CSRF |
| POST | /api/v1/auth/refresh | Renovação do acesso | CSRF e refresh cookie |
| POST | /api/v1/auth/logout | Encerramento da sessão | CSRF; identificação por cookie e/ou JWT válido |
| GET | /api/v1/usuarios/me | Usuário autenticado | Bearer JWT |
| POST | /api/v1/solicitacoes | Criação de solicitação | Bearer JWT |
| GET | /api/v1/solicitacoes/{id} | Consulta individual | Bearer JWT e propriedade |
| GET | /api/v1/solicitacoes | Listagem paginada e filtros | Bearer JWT e propriedade |
| PUT | /api/v1/solicitacoes/{id} | Edição somente em ABERTO | Bearer JWT e propriedade |
| DELETE | /api/v1/solicitacoes/{id} | Exclusão física somente em ABERTO | Bearer JWT e propriedade |
| PATCH | /api/v1/solicitacoes/{id}/status | Avanço de status | Bearer JWT e propriedade |
| GET | /api/v1/dashboard | Totais e contagens por status | Bearer JWT |
| GET | /v3/api-docs | Contrato OpenAPI | Público |
| GET | /swagger-ui/index.html | Interface Swagger | Público |

## Cadastro de usuário

Exemplo com credenciais exclusivamente de teste:

```bash
curl -i http://localhost:8080/api/v1/usuarios \
  -H 'Content-Type: application/json' \
  --data '{"name":"Ana","email":"ana.cadastro@example.com","password":"Teste12345!"}'
```

Respostas:

- 201: usuário cadastrado.
- 400: dados inválidos ou JSON inválido.
- 409: e-mail já cadastrado.
- 500: erro inesperado.

A resposta contém `id`, `name`, `email` e `createdAt`.
Senha e hash não são retornados.

O cadastro não autentica automaticamente o usuário.

### Regras de cadastro

- Nome: de 3 a 100 pontos de código Unicode após remover espaços externos.
- E-mail: válido, normalizado para minúsculas e único.
- Senha: mínimo de 8 pontos de código Unicode e máximo de 72 bytes em UTF-8.
- Senha sem remoção de espaços ou normalização.
- Hash BCrypt com custo 12.

## Autenticação

O login retorna um access token JWT e envia o refresh token
somente por cookie HttpOnly.

O access token possui validade máxima de 15 minutos,
limitada pela expiração da sessão.

A sessão possui validade absoluta de oito horas.
A renovação substitui o refresh token sem estender esse prazo.

Somente o hash SHA-256 do refresh token é armazenado no banco.
O JWT é assinado com HS256.

Cada acesso protegido verifica também se a sessão associada
está ativa e pertence ao usuário identificado pelo JWT.

### Logout

O logout revoga a sessão e remove o refresh cookie.

Aceita opcionalmente um Bearer JWT válido para identificar
a sessão mesmo quando o refresh cookie está desatualizado.

A sessão identificada pelo JWT tem seu proprietário verificado.
O fluxo existente de revogação pelo cookie também é executado.

Se JWT e cookie identificarem sessões diferentes,
ambas são revogadas.

Após a revogação, os JWTs associados à sessão são rejeitados.

Bearer inválido ou expirado retorna HTTP 401.
Chamadas somente por cookie devem omitir Authorization.

Logout somente com cookie antigo pode não identificar a sessão
após uma rotação. O frontend deverá coordenar renovação e logout
e descartar respostas atrasadas após o encerramento local.

### Proteção CSRF

Antes de login, renovação ou logout:

1. Consulte `GET /api/v1/auth/csrf`.
2. Preserve os cookies recebidos.
3. Envie o token retornado no header indicado por `headerName`.

A resposta informa `X-CSRF-TOKEN` como nome do header.

Obtenha novamente o token quando o contexto de autenticação
alterar o cookie CSRF. Os testes HTTP realizam essa obtenção
antes da renovação e do logout.

O refresh cookie utiliza HttpOnly, SameSite=Lax
e Path=/api/v1/auth.

No ambiente HTTP local, Secure permanece desativado.
Em HTTPS, habilite Secure por configuração.

## Criação de solicitações

```http
POST /api/v1/solicitacoes
Authorization: Bearer <access-token>
Content-Type: application/json
```

Corpo:

```json
{
  "titulo": "Acesso ao sistema",
  "descricao": "Preciso de acesso ao sistema interno.",
  "categoria": "TI"
}
```

Regras:

- Título obrigatório, com até 150 pontos de código Unicode.
- Descrição obrigatória, com até 5.000 pontos de código Unicode.
- Espaços externos removidos do título e da descrição.
- Categoria obrigatória: TI, RH, COMPRAS, FINANCEIRO ou INFRAESTRUTURA.
- Proprietário obtido exclusivamente do JWT validado.
- Status inicial sempre ABERTO.
- UUID gerado pelo domínio.
- Código numérico gerado pelo PostgreSQL.

Resposta de sucesso: HTTP 201.

O corpo contém `id`, `codigo`, `titulo`, `descricao`, `categoria`,
`status`, `solicitanteId`, `createdAt` e `updatedAt`.

A versão de persistência não é exposta.
O cliente não pode definir proprietário, status ou código.

## Consulta individual

```http
GET /api/v1/solicitacoes/{id}
Authorization: Bearer <access-token>
```

A busca combina identificador da solicitação e proprietário.

Respostas:

- 200: solicitação encontrada para o usuário autenticado.
- 400: identificador com formato inválido.
- 401: autenticação ausente ou inválida.
- 404: solicitação inexistente ou pertencente a outro usuário.

Solicitação inexistente e solicitação de outro usuário retornam
o mesmo código e mensagem de erro.

## Listagem e filtros

```http
GET /api/v1/solicitacoes?page=0&size=20&status=ABERTO&categoria=TI
Authorization: Bearer <access-token>
```

| Parâmetro | Regra |
|---|---|
| page | Índice iniciado em zero; padrão 0 |
| size | De 1 a 100; padrão 20 |
| status | Opcional: ABERTO, EM_ATENDIMENTO ou CONCLUIDO |
| categoria | Opcional: TI, RH, COMPRAS, FINANCEIRO ou INFRAESTRUTURA |

Os filtros são combinados com AND.
A ordenação é `createdAt DESC, codigo DESC`.

Conteúdo e totais consideram somente as solicitações do usuário
autenticado que correspondam aos filtros.

Formato da resposta:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Página além do intervalo retorna HTTP 200 com conteúdo vazio
e totais preservados.

Paginação inválida ou enum desconhecido retorna HTTP 400.
Ausência de autenticação retorna HTTP 401.

## Edição de solicitações

```http
PUT /api/v1/solicitacoes/{id}
Authorization: Bearer <access-token>
Content-Type: application/json
```

Corpo:

```json
{
  "titulo": "Título atualizado",
  "descricao": "Descrição atualizada",
  "categoria": "RH"
}
```

A edição exige solicitação própria com status ABERTO.
Título, descrição e categoria são obrigatórios.

São preservados identificador, código, proprietário e data de criação.
A data de atualização é alterada e a versão é incrementada.

Sucesso: HTTP 204, sem corpo.

## Exclusão de solicitações

```http
DELETE /api/v1/solicitacoes/{id}
Authorization: Bearer <access-token>
```

A exclusão exige solicitação própria com status ABERTO.
O registro é removido fisicamente.

Sucesso: HTTP 204, sem corpo.
Uma consulta posterior retorna HTTP 404.

## Alteração de status

```http
PATCH /api/v1/solicitacoes/{id}/status
Authorization: Bearer <access-token>
Content-Type: application/json
```

Corpo:

```json
{
  "status": "EM_ATENDIMENTO"
}
```

Transições permitidas: ABERTO → EM_ATENDIMENTO → CONCLUIDO.

Retornos, saltos de etapa e repetição do status atual são proibidos.

A alteração exige propriedade, preserva os dados descritivos,
atualiza `updatedAt` e incrementa a versão.

Sucesso: HTTP 204, sem corpo.

### Erros das operações

- 400: dados ou parâmetros inválidos.
- 401: autenticação ausente ou inválida.
- 404: solicitação inexistente ou pertencente a outro usuário.
- 409: estado incompatível ou conflito de escrita.

Código do conflito: `REQUEST_STATE_CONFLICT`.

Após uma alteração, o cliente pode consultar novamente a solicitação
para obter seu estado atualizado.

## Controle de concorrência das solicitações

As escritas de edição, exclusão e alteração de status verificam:

- Identificador.
- Proprietário.
- Versão esperada.
- Status esperado.

Atualizações incrementam a versão na mesma instrução de escrita.
Não há atualização genérica por `save` nessas operações.

O controle protege o intervalo entre leitura e escrita no backend.

Não há detecção específica de formulários antigos no cliente:
a versão esperada é consultada pelo backend durante a operação.

## Dashboard

```http
GET /api/v1/dashboard
Authorization: Bearer <access-token>
```

Considera somente as solicitações do usuário autenticado.

Resposta:

```json
{
  "total": 0,
  "abertas": 0,
  "emAtendimento": 0,
  "concluidas": 0
}
```

Os indicadores consideram todas as solicitações do usuário,
independentemente dos filtros da listagem.

Usuário sem solicitações recebe valores zero.
O total corresponde à soma das contagens por status.

A resposta utiliza `Cache-Control: no-store`.

As contagens são calculadas em uma única consulta ao PostgreSQL,
sem carregar as solicitações em memória.

## Documentação interativa da API

Com o backend em execução:

- [Swagger UI](http://localhost:8080/swagger-ui/index.html)
- [Contrato OpenAPI](http://localhost:8080/v3/api-docs)

A documentação é pública.
Os endpoints de negócio preservam suas exigências de autenticação,
propriedade e CSRF.

O contrato gerado utiliza OpenAPI 3.1.0.
A versão 3.1.1 identifica a dependência springdoc utilizada.

### Uso da autenticação no Swagger

1. Execute `GET /api/v1/auth/csrf`.
2. Copie o campo `token` retornado.
3. Abra Authorize e preencha `csrfToken`.
4. Execute o login.
5. Copie `accessToken` e preencha `bearerAuth`.

O navegador preserva os cookies HttpOnly.
Não é necessário preencher `refreshCookie` manualmente.

Se o cookie CSRF mudar, obtenha outro token e atualize `csrfToken`.

As credenciais informadas em Authorize não são persistidas
após recarregar a interface.

## Integração prevista com o frontend

O frontend utilizará `/api` na mesma origem do navegador:

- Proxy do Vite no desenvolvimento.
- Proxy do Nginx no ambiente Docker.
- Preservação do caminho `/api/v1` e dos cookies.

O access token será mantido em memória.
Após recarregar a página, o frontend poderá solicitar renovação
utilizando o refresh cookie e CSRF.

As operações de autenticação deverão ser coordenadas para evitar
renovações simultâneas e restauração indevida após logout.

A configuração dos proxies e a validação pelo navegador
ainda serão realizadas no repositório frontend.

## Testes

Com o Docker acessível:

```bash
./mvnw verify
```

Os testes de integração iniciam PostgreSQL temporário por Testcontainers.
Não utilizam o banco do Docker Compose.

O perfil `test` utiliza uma chave JWT conhecida e exclusiva para testes.
Essa chave não deve ser utilizada em outros ambientes.

Para executar testes específicos:

```bash
./mvnw -Dtest=CadastroUsuarioIntegrationTest test
./mvnw -Dtest=AutenticacaoHttpIntegrationTest test
./mvnw -Dtest=SolicitacaoRepositoryIntegrationTest test
./mvnw -Dtest=CriarSolicitacaoHttpIntegrationTest test
./mvnw -Dtest=ConsultarSolicitacaoHttpIntegrationTest test
./mvnw -Dtest=ListarSolicitacoesHttpIntegrationTest test
./mvnw -Dtest=SolicitacaoOperacoesTest test
./mvnw -Dtest=OperacoesSolicitacaoHttpIntegrationTest test
./mvnw -Dtest=SolicitacaoOperacoesRepositoryIntegrationTest test
./mvnw -Dtest=DashboardHttpIntegrationTest test
./mvnw -Dtest=LogoutAposRotacaoHttpIntegrationTest test
```

### Validação local mais recente

- Suíte completa: 146 testes.
- Nenhuma falha, erro ou teste ignorado.
- Maven verify: BUILD SUCCESS.
- Autenticação HTTP: cinco execuções aprovadas.
- Criação de solicitações HTTP: dez execuções aprovadas.
- Consulta individual HTTP: cinco testes aprovados.
- Listagem HTTP: onze execuções aprovadas.
- Operações de solicitações HTTP: quatorze execuções aprovadas.
- Condições de escrita: três testes de persistência aprovados.
- Dashboard HTTP: quatro testes aprovados.
- Logout após rotação: um teste de regressão aprovado.
- Persistência e recuperação do código gerado verificadas.
- Revogação da sessão e rejeição dos JWTs após logout verificadas.
- Logout com cookie antigo e JWT válido verificado.
- Isolamento de consultas, listagem, totais e dashboard verificado.
- Paginação, ordenação e filtros individuais e combinados verificados.
- Edição, exclusão física e transições de status verificadas.
- Escritas com versão antiga, status incompatível e proprietário
  incorreto recusadas sem alteração indevida do registro.
- Swagger UI acessível no navegador e por GET com HTTP 200.
- Contrato OpenAPI com dez caminhos e códigos 201/204 conferidos.
- Dashboard sem JWT continua retornando HTTP 401.

Os testes de condições de escrita e logout após rotação
reproduzem os cenários sequencialmente, sem múltiplas threads.

Resultados informados pelo desenvolvedor a partir da execução
no Ubuntu/WSL.

## Integração contínua

O GitHub Actions executa:

1. Verificação Maven.
2. Build da imagem Docker.

O build Docker utiliza `-DskipTests` porque os testes são executados
na verificação Maven.

Não há publicação automática de imagens nem deploy automático.

A execução local não substitui a validação da CI.
O resultado da CI da branch de revisão da integração
será registrado após sua execução.

## Limitações atuais

- Frontend e integração completa pelos proxies pendentes.
- Coordenação de renovação e logout no frontend pendente.
- Logout somente com refresh cookie antigo pode não identificar
  a sessão após rotação.
- Bearer inválido ou expirado impede a execução do logout com esse header.
- Ordenação fixa e ausência de busca textual.
- Testes de persistência não simulam múltiplas threads concorrentes.
- Não há detecção específica de formulários antigos no cliente.
- Política explícita para campos JSON desconhecidos ainda pendente.
- Campos desconhecidos não controlam proprietário ou status na criação.
- Respostas de erro da segurança e dos controllers possuem
  diferenças de estrutura.
- OpenAPI documenta códigos e descrições de erros,
  mas seus schemas ainda não foram detalhados na configuração.
- Memorial Técnico ainda será consolidado.

## Documentação

- [Infraestrutura inicial](docs/devops/estrutura-inicial.md)
- [Persistência e migrations](docs/database/persistencia-migrations.md)
- [Arquitetura do cadastro](docs/architecture/cadastro-usuario.md)
- [Arquitetura da autenticação](docs/architecture/autenticacao-jwt.md)
- [Arquitetura da criação](docs/architecture/criacao-solicitacoes.md)
- [Arquitetura das consultas](docs/architecture/consulta-solicitacoes.md)
- [Arquitetura das operações](docs/architecture/operacoes-solicitacoes.md)
- [Arquitetura do dashboard](docs/architecture/dashboard-usuario.md)
- [Documentação da API](docs/architecture/documentacao-api.md)
- [Revisão da integração](docs/architecture/revisao-integracao-backend.md)
- [Registro de LLM: estrutura inicial](docs/llm/estrutura-inicial.md)
- [Registro de LLM: persistência](docs/llm/persistencia-migrations.md)
- [Registro de LLM: cadastro](docs/llm/cadastro-usuario.md)
- [Registro de LLM: autenticação](docs/llm/autenticacao-jwt.md)
- [Registro de LLM: criação](docs/llm/criacao-solicitacoes.md)
- [Registro de LLM: consultas](docs/llm/consulta-solicitacoes.md)
- [Registro de LLM: operações](docs/llm/operacoes-solicitacoes.md)
- [Registro de LLM: dashboard](docs/llm/dashboard-usuario.md)
- [Registro de LLM: documentação da API](docs/llm/documentacao-api.md)
- [Registro de LLM: revisão da integração](docs/llm/revisao-integracao-backend.md)

O Memorial Técnico de Desenvolvimento será consolidado
durante a integração e preparação da entrega.

## Frontend

Repositório separado:

[portal-solicitacoes-frontend](https://github.com/JoseWenned/portal-solicitacoes-frontend)