package br.com.wenned.portalsolicitacoes.presentation.controller.dashboard;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
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
class DashboardHttpIntegrationTest {

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

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void deveContabilizarStatusSomenteDoUsuarioAutenticado() throws Exception {
        var usuario = cadastrarEAutenticar();
        var outroUsuario = cadastrarEAutenticar();

        inserirSolicitacao(usuario.id(), "ABERTO");
        inserirSolicitacao(usuario.id(), "ABERTO");
        inserirSolicitacao(usuario.id(), "EM_ATENDIMENTO");
        inserirSolicitacao(usuario.id(), "CONCLUIDO");
        inserirSolicitacao(usuario.id(), "CONCLUIDO");
        inserirSolicitacao(usuario.id(), "CONCLUIDO");

        inserirSolicitacao(outroUsuario.id(), "ABERTO");
        inserirSolicitacao(outroUsuario.id(), "EM_ATENDIMENTO");
        inserirSolicitacao(outroUsuario.id(), "EM_ATENDIMENTO");

        var response = consultar(usuario);

        verificarContagens(response, 6, 2, 1, 3);

        assertThat(response.headers().firstValue("cache-control"))
            .hasValue("no-store");

        verificarContagens(consultar(outroUsuario), 3, 1, 2, 0);
    }

    @Test
    void deveRetornarZerosSemConsiderarSolicitacoesDeOutroUsuario()
        throws Exception {

        var usuarioSemSolicitacoes = cadastrarEAutenticar();
        var outroUsuario = cadastrarEAutenticar();

        inserirSolicitacao(outroUsuario.id(), "ABERTO");
        inserirSolicitacao(outroUsuario.id(), "CONCLUIDO");

        verificarContagens(
            consultar(usuarioSemSolicitacoes),
            0,
            0,
            0,
            0
        );
    }

    @Test
    void deveAtualizarIndicadoresAposAlteracaoDeStatusEExclusao()
        throws Exception {

        var usuario = cadastrarEAutenticar();
        UUID id = criarSolicitacao(usuario);

        verificarContagens(consultar(usuario), 1, 1, 0, 0);

        var atendimento = enviar(
            usuario.client(),
            "PATCH",
            "/solicitacoes/" + id + "/status",
            "{\"status\":\"EM_ATENDIMENTO\"}",
            null,
            usuario.jwt()
        );

        verificarStatus(atendimento, 204);
        verificarContagens(consultar(usuario), 1, 0, 1, 0);

        var conclusao = enviar(
            usuario.client(),
            "PATCH",
            "/solicitacoes/" + id + "/status",
            "{\"status\":\"CONCLUIDO\"}",
            null,
            usuario.jwt()
        );

        verificarStatus(conclusao, 204);
        verificarContagens(consultar(usuario), 1, 0, 0, 1);

        UUID aberta = criarSolicitacao(usuario);
        verificarContagens(consultar(usuario), 2, 1, 0, 1);

        var exclusao = enviar(
            usuario.client(),
            "DELETE",
            "/solicitacoes/" + aberta,
            null,
            null,
            usuario.jwt()
        );

        verificarStatus(exclusao, 204);
        verificarContagens(consultar(usuario), 1, 0, 0, 1);
    }

    @Test
    void deveRecusarDashboardSemAutenticacao() throws Exception {
        var response = enviar(
            novoCliente(),
            "GET",
            "/dashboard",
            null,
            null,
            null
        );

        verificarStatus(response, 401);
    }

    private void inserirSolicitacao(UUID usuarioId, String status) {
        jdbcTemplate.update("""
            INSERT INTO solicitacoes (
                id, titulo, descricao, categoria, status, solicitante_id
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            UUID.randomUUID(),
            "Solicitação de teste",
            "Descrição da solicitação de teste.",
            "TI",
            status,
            usuarioId
        );
    }

    private HttpResponse<String> consultar(UsuarioAutenticado usuario)
        throws Exception {

        return enviar(
            usuario.client(),
            "GET",
            "/dashboard",
            null,
            null,
            usuario.jwt()
        );
    }

    private void verificarContagens(
        HttpResponse<String> response,
        long total,
        long abertas,
        long emAtendimento,
        long concluidas
    ) {
        verificarStatus(response, 200);

        Number actualTotal = JsonPath.read(response.body(), "$.total");
        Number actualAbertas = JsonPath.read(response.body(), "$.abertas");
        Number actualAtendimento = JsonPath.read(
            response.body(),
            "$.emAtendimento"
        );
        Number actualConcluidas = JsonPath.read(
            response.body(),
            "$.concluidas"
        );

        assertThat(actualTotal.longValue()).isEqualTo(total);
        assertThat(actualAbertas.longValue()).isEqualTo(abertas);
        assertThat(actualAtendimento.longValue()).isEqualTo(emAtendimento);
        assertThat(actualConcluidas.longValue()).isEqualTo(concluidas);

        assertThat(actualTotal.longValue()).isEqualTo(
            actualAbertas.longValue()
                + actualAtendimento.longValue()
                + actualConcluidas.longValue()
        );
    }

    private void verificarStatus(
        HttpResponse<String> response,
        int esperado
    ) {
        assertThat(response.statusCode())
            .as("Resposta HTTP: %s", response.body())
            .isEqualTo(esperado);
    }

    private UUID criarSolicitacao(UsuarioAutenticado usuario)
        throws Exception {

        var response = enviar(
            usuario.client(),
            "POST",
            "/solicitacoes",
            """
            {
              "titulo":"Solicitação para dashboard",
              "descricao":"Descrição da solicitação.",
              "categoria":"TI"
            }
            """,
            null,
            usuario.jwt()
        );

        verificarStatus(response, 201);

        return UUID.fromString(JsonPath.read(response.body(), "$.id"));
    }

    private UsuarioAutenticado cadastrarEAutenticar() throws Exception {
        HttpClient client = novoCliente();
        String email = "dashboard-" + UUID.randomUUID() + "@example.com";

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

        verificarStatus(cadastro, 201);

        UUID id = UUID.fromString(
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

        verificarStatus(csrfResponse, 200);

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

        verificarStatus(login, 200);

        String jwt = JsonPath.read(login.body(), "$.accessToken");

        return new UsuarioAutenticado(client, id, jwt);
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

        if (body != null) {
            builder.header("Content-Type", "application/json");
        }

        builder.method(
            method,
            body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)
        );

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