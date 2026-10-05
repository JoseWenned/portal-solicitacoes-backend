package br.com.wenned.portalsolicitacoes.presentation.controller.solicitacoes;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class CriarSolicitacaoHttpIntegrationTest {

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
    void deveCriarSolicitacaoVinculadaAoUsuarioAutenticado() throws Exception {
        var autenticado = cadastrarEAutenticar();

        var response = enviar(
            autenticado.client(),
            "POST",
            "/solicitacoes",
            """
            {
              "titulo": "  Acesso ao sistema  ",
              "descricao": "  Preciso de acesso ao sistema interno.  ",
              "categoria": "TI"
            }
            """,
            null,
            autenticado.jwt()
        );

        assertThat(response.statusCode())
            .as("Resposta da criação: %s", response.body())
            .isEqualTo(201);

        UUID id = UUID.fromString(
            JsonPath.read(response.body(), "$.id")
        );

        Number codigo = JsonPath.read(response.body(), "$.codigo");
        String titulo = JsonPath.read(response.body(), "$.titulo");
        String descricao = JsonPath.read(response.body(), "$.descricao");
        String categoria = JsonPath.read(response.body(), "$.categoria");
        String status = JsonPath.read(response.body(), "$.status");
        String solicitanteId = JsonPath.read(
            response.body(),
            "$.solicitanteId"
        );
        String createdAt = JsonPath.read(response.body(), "$.createdAt");
        String updatedAt = JsonPath.read(response.body(), "$.updatedAt");

        assertThat(codigo.longValue()).isPositive();
        assertThat(titulo).isEqualTo("Acesso ao sistema");
        assertThat(descricao)
            .isEqualTo("Preciso de acesso ao sistema interno.");
        assertThat(categoria).isEqualTo("TI");
        assertThat(status).isEqualTo("ABERTO");
        assertThat(solicitanteId)
            .isEqualTo(autenticado.usuarioId().toString());
        assertThat(createdAt).isNotBlank();
        assertThat(updatedAt).isEqualTo(createdAt);
        assertThat(response.body()).doesNotContain("\"version\"");

        jdbcTemplate.queryForObject("""
            SELECT codigo, solicitante_id, status, titulo, descricao
            FROM solicitacoes
            WHERE id = ?
            """,
            (rs, rowNum) -> {
                assertThat(rs.getLong("codigo"))
                    .isEqualTo(codigo.longValue());
                assertThat(rs.getObject("solicitante_id", UUID.class))
                    .isEqualTo(autenticado.usuarioId());
                assertThat(rs.getString("status")).isEqualTo("ABERTO");
                assertThat(rs.getString("titulo")).isEqualTo(titulo);
                assertThat(rs.getString("descricao")).isEqualTo(descricao);

                return true;
            },
            id
        );
    }

    @Test
    void naoDevePermitirQueOClienteEscolhaProprietarioOuStatus()
        throws Exception {

        var autenticado = cadastrarEAutenticar();
        UUID outroUsuarioId = cadastrarUsuario(autenticado.client());

        var response = enviar(
            autenticado.client(),
            "POST",
            "/solicitacoes",
            """
            {
              "titulo": "Compra de teclado",
              "descricao": "Solicito um teclado para minha estação.",
              "categoria": "COMPRAS",
              "solicitanteId": "%s",
              "status": "CONCLUIDO",
              "codigo": 999
            }
            """.formatted(outroUsuarioId),
            null,
            autenticado.jwt()
        );

        // Campos desconhecidos podem ser rejeitados ou ignorados.
        // Em nenhum dos casos podem controlar o registro criado.
        assertThat(response.statusCode()).isIn(201, 400);

        if (response.statusCode() == 400) {
            assertThat(quantidadeSolicitacoes(autenticado.usuarioId()))
                .isZero();
        } else {
            UUID id = UUID.fromString(
                JsonPath.read(response.body(), "$.id")
            );

            UUID proprietario = jdbcTemplate.queryForObject("""
                SELECT solicitante_id FROM solicitacoes WHERE id = ?
                """, UUID.class, id);

            String status = jdbcTemplate.queryForObject("""
                SELECT status FROM solicitacoes WHERE id = ?
                """, String.class, id);

            assertThat(proprietario).isEqualTo(autenticado.usuarioId());
            assertThat(status).isEqualTo("ABERTO");
        }

        assertThat(quantidadeSolicitacoes(outroUsuarioId)).isZero();
    }

    @Test
    void deveRecusarCriacaoSemAutenticacao() throws Exception {
        var response = enviar(
            novoCliente(),
            "POST",
            "/solicitacoes",
            """
            {
              "titulo": "Acesso ao sistema",
              "descricao": "Preciso de acesso.",
              "categoria": "TI"
            }
            """,
            null,
            null
        );

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        """
        {"titulo":" ","descricao":"Descrição válida","categoria":"TI"}
        """,
        """
        {"titulo":"Título válido","descricao":" ","categoria":"TI"}
        """,
        """
        {"titulo":"Título válido","descricao":"Descrição válida"}
        """,
        """
        {"titulo":"Título válido","descricao":"Descrição válida","categoria":"INVALIDA"}
        """
    })
    void deveRecusarDadosInvalidosSemPersistir(String body) throws Exception {
        var autenticado = cadastrarEAutenticar();

        var response = enviar(
            autenticado.client(),
            "POST",
            "/solicitacoes",
            body,
            null,
            autenticado.jwt()
        );

        assertThat(response.statusCode())
            .as("Resposta para dados inválidos: %s", response.body())
            .isEqualTo(400);

        assertThat(quantidadeSolicitacoes(autenticado.usuarioId())).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"titulo", "descricao"})
    void deveRecusarTextoAcimaDoLimiteSemPersistir(String campo)
        throws Exception {

        var autenticado = cadastrarEAutenticar();

        String titulo = campo.equals("titulo")
            ? "a".repeat(151)
            : "Título válido";

        String descricao = campo.equals("descricao")
            ? "a".repeat(5001)
            : "Descrição válida";

        var response = enviar(
            autenticado.client(),
            "POST",
            "/solicitacoes",
            """
            {
              "titulo": "%s",
              "descricao": "%s",
              "categoria": "TI"
            }
            """.formatted(titulo, descricao),
            null,
            autenticado.jwt()
        );

        assertThat(response.statusCode())
            .as("Resposta para limite excedido: %s", response.body())
            .isEqualTo(400);

        String code = JsonPath.read(response.body(), "$.code");
        assertThat(code).isEqualTo("VALIDATION_ERROR");

        assertThat(quantidadeSolicitacoes(autenticado.usuarioId())).isZero();
    }

    @Test
    void deveRecusarJsonMalformadoSemPersistir() throws Exception {
        var autenticado = cadastrarEAutenticar();

        var response = enviar(
            autenticado.client(),
            "POST",
            "/solicitacoes",
            "{\"titulo\":",
            null,
            autenticado.jwt()
        );

        assertThat(response.statusCode()).isEqualTo(400);

        String code = JsonPath.read(response.body(), "$.code");
        assertThat(code).isEqualTo("INVALID_REQUEST_BODY");

        assertThat(quantidadeSolicitacoes(autenticado.usuarioId())).isZero();
    }

    private UsuarioAutenticado cadastrarEAutenticar() throws Exception {
        HttpClient client = novoCliente();
        String email = "solicitacao-" + UUID.randomUUID() + "@example.com";
        UUID usuarioId = cadastrarUsuario(client, email);

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

    private UUID cadastrarUsuario(HttpClient client) throws Exception {
        return cadastrarUsuario(
            client,
            "outro-" + UUID.randomUUID() + "@example.com"
        );
    }

    private UUID cadastrarUsuario(HttpClient client, String email)
        throws Exception {

        var response = enviar(
            client,
            "POST",
            "/usuarios",
            """
            {"name":"Ana","email":"%s","password":"Teste12345!"}
            """.formatted(email),
            null,
            null
        );

        assertThat(response.statusCode())
            .as("Resposta do cadastro: %s", response.body())
            .isEqualTo(201);

        return UUID.fromString(JsonPath.read(response.body(), "$.id"));
    }

    private Long quantidadeSolicitacoes(UUID usuarioId) {
        return jdbcTemplate.queryForObject("""
            SELECT count(*)
            FROM solicitacoes
            WHERE solicitante_id = ?
            """, Long.class, usuarioId);
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
        UUID usuarioId,
        String jwt
    ) {
        @Override
        public String toString() {
            return "UsuarioAutenticado[usuarioId=" + usuarioId + "]";
        }
    }
}