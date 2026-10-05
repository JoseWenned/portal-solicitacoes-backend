package br.com.wenned.portalsolicitacoes.presentation.controller.solicitacoes;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class ConsultarSolicitacaoHttpIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    int port;

    @Test
    void deveConsultarSolicitacaoDoUsuarioAutenticado() throws Exception {
        var usuario = cadastrarEAutenticar();
        UUID solicitacaoId = criarSolicitacao(usuario);

        var response = consultar(usuario, solicitacaoId.toString());

        assertThat(response.statusCode())
            .as("Resposta da consulta: %s", response.body())
            .isEqualTo(200);

        String id = JsonPath.read(response.body(), "$.id");
        String solicitanteId = JsonPath.read(
            response.body(),
            "$.solicitanteId"
        );
        String titulo = JsonPath.read(response.body(), "$.titulo");
        String descricao = JsonPath.read(response.body(), "$.descricao");
        String categoria = JsonPath.read(response.body(), "$.categoria");
        String status = JsonPath.read(response.body(), "$.status");
        Number codigo = JsonPath.read(response.body(), "$.codigo");
        String createdAt = JsonPath.read(response.body(), "$.createdAt");
        String updatedAt = JsonPath.read(response.body(), "$.updatedAt");

        assertThat(id).isEqualTo(solicitacaoId.toString());
        assertThat(solicitanteId).isEqualTo(usuario.id().toString());
        assertThat(titulo).isEqualTo("Acesso ao sistema");
        assertThat(descricao).isEqualTo("Preciso de acesso ao sistema interno.");
        assertThat(categoria).isEqualTo("TI");
        assertThat(status).isEqualTo("ABERTO");
        assertThat(codigo.longValue()).isPositive();
        assertThat(createdAt).isNotBlank();
        assertThat(updatedAt).isEqualTo(createdAt);
        assertThat(response.body()).doesNotContain("\"version\"");
    }

    @Test
    void deveRetornar404ParaSolicitacaoDeOutroUsuario() throws Exception {
        var proprietario = cadastrarEAutenticar();
        UUID solicitacaoId = criarSolicitacao(proprietario);

        var outroUsuario = cadastrarEAutenticar();
        var response = consultar(outroUsuario, solicitacaoId.toString());

        assertThat(response.statusCode())
            .as("Resposta ao consultar solicitação alheia: %s", response.body())
            .isEqualTo(404);

        String code = JsonPath.read(response.body(), "$.code");
        String message = JsonPath.read(response.body(), "$.message");

        assertThat(code).isEqualTo("REQUEST_NOT_FOUND");
        assertThat(message).isEqualTo("Solicitação não encontrada.");

        assertThat(response.body()).doesNotContain(
            proprietario.id().toString(),
            "Acesso ao sistema",
            "Preciso de acesso ao sistema interno."
        );

        // O proprietário continua conseguindo consultar o mesmo registro.
        assertThat(
            consultar(proprietario, solicitacaoId.toString()).statusCode()
        ).isEqualTo(200);
    }

    @Test
    void deveRetornar404ParaSolicitacaoInexistente() throws Exception {
        var usuario = cadastrarEAutenticar();

        var response = consultar(usuario, UUID.randomUUID().toString());

        assertThat(response.statusCode()).isEqualTo(404);

        String code = JsonPath.read(response.body(), "$.code");
        String message = JsonPath.read(response.body(), "$.message");

        assertThat(code).isEqualTo("REQUEST_NOT_FOUND");
        assertThat(message).isEqualTo("Solicitação não encontrada.");
    }

    @Test
    void deveRecusarIdentificadorComFormatoInvalido() throws Exception {
        var usuario = cadastrarEAutenticar();

        var response = consultar(usuario, "identificador-invalido");

        assertThat(response.statusCode())
            .as("Resposta para UUID inválido: %s", response.body())
            .isEqualTo(400);

        String code = JsonPath.read(response.body(), "$.code");
        String campo = JsonPath.read(
            response.body(),
            "$.fieldErrors[0].field"
        );

        assertThat(code).isEqualTo("INVALID_PARAMETER");
        assertThat(campo).isEqualTo("id");
    }

    @Test
    void deveRecusarConsultaSemAutenticacao() throws Exception {
        var response = enviar(
            novoCliente(),
            "GET",
            "/solicitacoes/" + UUID.randomUUID(),
            null,
            null,
            null
        );

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> consultar(
        UsuarioAutenticado usuario,
        String id
    ) throws Exception {
        return enviar(
            usuario.client(),
            "GET",
            "/solicitacoes/" + id,
            null,
            null,
            usuario.jwt()
        );
    }

    private UUID criarSolicitacao(UsuarioAutenticado usuario)
        throws Exception {

        var response = enviar(
            usuario.client(),
            "POST",
            "/solicitacoes",
            """
            {
              "titulo": "Acesso ao sistema",
              "descricao": "Preciso de acesso ao sistema interno.",
              "categoria": "TI"
            }
            """,
            null,
            usuario.jwt()
        );

        assertThat(response.statusCode())
            .as("Resposta da criação: %s", response.body())
            .isEqualTo(201);

        return UUID.fromString(JsonPath.read(response.body(), "$.id"));
    }

    private UsuarioAutenticado cadastrarEAutenticar() throws Exception {
        HttpClient client = novoCliente();
        String email = "consulta-" + UUID.randomUUID() + "@example.com";

        var cadastro = enviar(
            client,
            "POST",
            "/usuarios",
            """
            {"name":"Ana","email":"%s","password":"Teste12345!"}
            """.formatted(email),
            null,
            null
        );

        assertThat(cadastro.statusCode())
            .as("Resposta do cadastro: %s", cadastro.body())
            .isEqualTo(201);

        UUID usuarioId = UUID.fromString(
            JsonPath.read(cadastro.body(), "$.id")
        );

        var csrfResponse = enviar(
            client,
            "GET",
            "/auth/csrf",
            null,
            null,
            null
        );

        assertThat(csrfResponse.statusCode()).isEqualTo(200);

        String csrf = JsonPath.read(csrfResponse.body(), "$.token");

        var login = enviar(
            client,
            "POST",
            "/auth/login",
            """
            {"email":"%s","password":"Teste12345!"}
            """.formatted(email),
            csrf,
            null
        );

        assertThat(login.statusCode())
            .as("Resposta do login: %s", login.body())
            .isEqualTo(200);

        String jwt = JsonPath.read(login.body(), "$.accessToken");

        return new UsuarioAutenticado(client, usuarioId, jwt);
    }

    private HttpClient novoCliente() {
        return HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    private HttpResponse<String> enviar(
        HttpClient client,
        String method,
        String endpoint,
        String body,
        String csrf,
        String jwt
    ) throws Exception {
        var builder = HttpRequest.newBuilder()
            .uri(URI.create(
                "http://localhost:" + port + "/api/v1" + endpoint
            ))
            .timeout(Duration.ofSeconds(30));

        if (csrf != null) {
            builder.header("X-CSRF-TOKEN", csrf);
        }

        if (jwt != null) {
            builder.header("Authorization", "Bearer " + jwt);
        }

        if ("GET".equals(method)) {
            builder.GET();
        } else {
            builder.header("Content-Type", "application/json");
            builder.POST(body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        }

        return client.send(
            builder.build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private record UsuarioAutenticado(
        HttpClient client,
        UUID id,
        String jwt
    ) {
        @Override
        public String toString() {
            return "UsuarioAutenticado[id=" + id + "]";
        }
    }
}