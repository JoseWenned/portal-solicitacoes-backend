# Portal de Solicitações Internas — Backend

API do mini-projeto Full Stack desenvolvido para o processo seletivo
de Desenvolvedor de Sistemas Júnior da bit Soluções.

## Estado atual

Base Spring Boot implementada.

Disponível:
- Build e verificação pelo Maven Wrapper.
- Endpoint de saúde.
- Dockerfile com build em múltiplos estágios.
- Docker Compose com backend e PostgreSQL.
- CI com verificação Maven e build da imagem Docker.

Ainda não implementado:
- Conexão da aplicação ao banco e migrations.
- Cadastro e autenticação.
- Solicitações e dashboard.

## Tecnologias desta etapa

- Java 21.
- Spring Boot 4.1.1.
- Maven Wrapper, configurado para Maven 3.9.16.
- Spring Web MVC, Validation e Actuator.
- Docker, Docker Compose e PostgreSQL 16.
- GitHub Actions.

## Execução local

Pré-requisito: JDK 21.

```bash
chmod +x mvnw
./mvnw spring-boot:run