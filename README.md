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
- Tratamento padronizado dos erros do cadastro.
- Testes unitários e de integração com Testcontainers.
- Dockerfile e Docker Compose.
- Workflow de CI para verificação Maven e build Docker.

Ainda não implementado:

- Login, JWT, refresh token e logout.
- Funcionalidades de solicitações.
- Filtros e dashboard.
- Documentação OpenAPI/Swagger.

As tabelas de solicitações e sessões já existem,
mas suas funcionalidades ainda não estão disponíveis.

A CI das etapas anteriores foi validada.
- CI do PR de cadastro: concluída com sucesso, conforme resultado
  informado pelo desenvolvedor.

## Tecnologias utilizadas

- Java 21 e Spring Boot 4.1.1.
- Maven Wrapper, configurado para Maven 3.9.16.
- Spring Web MVC, Bean Validation e Actuator.
- Spring Data JPA e Hibernate.
- PostgreSQL 16 e Flyway.
- Spring Security Crypto com BCrypt.
- JUnit, Mockito e Testcontainers.
- Docker, Docker Compose e GitHub Actions.

## Organização do código

O backend é organizado por camadas:

- domain: entidades e regras de domínio.
- application: casos de uso e portas.
- infrastructure: persistência, mapeamento, hash e configuração.
- presentation: controllers, DTOs e tratamento de erros HTTP.

Dentro das camadas, os arquivos são agrupados por responsabilidade
e conceito de negócio.

Domínio e aplicação permanecem independentes de Spring e JPA.

## Pré-requisitos

Para executar pelo Docker Compose:

- Docker com daemon acessível.
- Docker Compose.

Para executar o Java diretamente no Ubuntu/WSL:

- JDK 21.
- PostgreSQL disponível, que pode ser iniciado pelo Compose.
- Acesso à internet na primeira execução do Maven Wrapper.

Não é necessária uma instalação global do Maven.

## Configuração

Crie o arquivo local de configuração:

```bash
cp .env.example .env
```

Se o arquivo .env já existir, preserve-o e confira seus valores.

Configure uma senha local para o PostgreSQL.
O arquivo .env não deve ser versionado.

| Variável do Compose | Finalidade |
|---|---|
| BACKEND_PORT | Porta local do backend; padrão 8080 |
| POSTGRES_PORT | Porta local do banco; padrão 5434 |
| POSTGRES_DB | Nome do banco |
| POSTGRES_USER | Usuário do banco |
| POSTGRES_PASSWORD | Senha do banco |

A aplicação recebe DB_URL, DB_USERNAME e DB_PASSWORD.
O Compose fornece essas variáveis ao backend.

O Spring Boot não carrega o arquivo .env automaticamente.

## Execução com Docker Compose

```bash
docker compose up --build -d
docker compose ps
```

O backend conecta-se a database:5432 na rede interna.
O banco fica acessível localmente em 127.0.0.1:5434 por padrão.

O Flyway aplica as migrations na inicialização.
O Hibernate está configurado com ddl-auto=validate.

Verifique a saúde:

```bash
curl --fail http://localhost:8080/actuator/health
```

Resultado esperado: status UP.

Se BACKEND_PORT foi alterada, ajuste a URL.

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

Exporte as variáveis usando os mesmos valores configurados no .env.
O exemplo abaixo considera banco e usuário com os nomes padrão:

```bash
export DB_URL='jdbc:postgresql://localhost:5434/portal_solicitacoes'
export DB_USERNAME='portal_app'
read -r -s -p 'Senha do banco: ' DB_PASSWORD
echo
export DB_PASSWORD
```

Execute:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Se você alterou a porta, o nome do banco ou o usuário,
ajuste as variáveis correspondentes.

## Cadastro de usuário

Endpoint:

```http
POST /api/v1/usuarios
```

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

A resposta contém id, name, email e createdAt.
Senha e hash não são retornados.

O cadastro não autentica automaticamente o usuário.
Login ainda não está disponível.

### Regras de cadastro

- Nome: de 3 a 100 caracteres Unicode após remover espaços externos.
- E-mail: válido, normalizado para minúsculas e único.
- Senha: mínimo de 8 caracteres Unicode e máximo de 72 bytes em UTF-8.
- Senha armazenada com BCrypt, custo 12.

## Testes

Com o Docker acessível:

```bash
./mvnw verify
```

Os testes de integração iniciam PostgreSQL temporário por Testcontainers.
Não utilizam o banco do Docker Compose.

Para executar apenas os testes de integração do cadastro:

```bash
./mvnw -Dtest=CadastroUsuarioIntegrationTest test
```

### Validação local da etapa de cadastro

- Cadastro manual: HTTP 201.
- Testes de integração do cadastro: 6 aprovados.
- Suíte completa: 28 testes, sem falhas, erros ou ignorados.
- Maven verify: BUILD SUCCESS.

## Integração contínua

O GitHub Actions executa:

1. Verificação Maven.
2. Build da imagem Docker.

O build Docker utiliza -DskipTests porque os testes são executados
no job de verificação Maven.

Não há publicação automática de imagens nem deploy automático.

## Documentação

- [Infraestrutura inicial](docs/devops/estrutura-inicial.md)
- [Persistência e migrations](docs/database/persistencia-migrations.md)
- [Arquitetura do cadastro](docs/architecture/cadastro-usuario.md)
- [Registro de LLM: estrutura inicial](docs/llm/estrutura-inicial.md)
- [Registro de LLM: persistência](docs/llm/persistencia-migrations.md)
- [Registro de LLM: cadastro](docs/llm/cadastro-usuario.md)

O Memorial Técnico de Desenvolvimento será consolidado
ao longo das próximas etapas.

## Frontend

Repositório separado:

https://github.com/JoseWenned/portal-solicitacoes-frontend