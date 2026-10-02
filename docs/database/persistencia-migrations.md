# Persistência e migrations

## Implementação

- PostgreSQL 16.
- Spring Data JPA e Hibernate para integração de persistência.
- Flyway para criação e evolução da estrutura.
- Hibernate configurado com ddl-auto=validate.
- Open Session in View desabilitado.
- Datas armazenadas com TIMESTAMPTZ.

## Estrutura

| Tabela | Finalidade | Relacionamento |
|---|---|---|
| usuarios | Identidade e hash da senha | Possui solicitações e sessões |
| solicitacoes | Demandas internas | solicitante_id referencia usuarios.id |
| sessoes_autenticacao | Renovação e revogação de acesso | usuario_id referencia usuarios.id |

As definições completas dos campos e restrições estão nas migrations
V1, V2 e V3 em src/main/resources/db/migration.

## Configuração

A aplicação recebe DB_URL, DB_USERNAME e DB_PASSWORD pelo ambiente.

No Compose, conecta-se a database:5432.
Para execução no WSL, o banco é publicado em 127.0.0.1:5434
por padrão.

O arquivo .env é utilizado pelo Compose.
O Spring Boot não carrega esse arquivo automaticamente.

## Testes

O teste de inicialização utiliza PostgreSQL temporário por Testcontainers
e verifica a aplicação das três migrations em banco vazio.

## Limitações

Entidades JPA, repositórios e regras de negócio ainda não implementados.
A configuração validate verificará os mapeamentos quando forem adicionados.

## Validação

- Maven verify: BUILD SUCCESS informado pelo desenvolvedor.
- Build da imagem Docker: sucesso.
- Histórico Flyway: migrations V1, V2 e V3 aplicadas com sucesso.
- Endpoint /actuator/health: status UP.
- GitHub Actions desta etapa: pendente.

A porta local do PostgreSQL foi ajustada para 5434 devido a conflito
com a porta 5433. Entre containers, permanece database:5432.