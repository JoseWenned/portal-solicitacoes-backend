package br.com.wenned.portalsolicitacoes.presentation.controller.autenticacao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
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
class LogoutAposRotacaoHttpIntegrationTest {

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
    JwtDecoder jwtDecoder;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void deveRevogarSessaoComJwtMesmoQuandoRefreshCookieEstaDesatualizado()
        throws Exception {

        CookieManager cookies = new CookieManager(
            null,
            CookiePolicy.ACCEPT_ALL
        );

        HttpClient client = HttpClient.newBuilder()
            .cookieHandler(cookies)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        String email = "logout-rotacao-" + UUID.randomUUID() + "@example.com";

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

        String csrf = obterCsrf(client);

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

        String primeiroJwt = JsonPath.read(login.body(), "$.accessToken");

        UUID sessaoId = UUID.fromString(
            jwtDecoder.decode(primeiroJwt).getClaimAsString("sid")
        );

        HttpCookie cookieAntigo = (HttpCookie) refreshCookie(cookies).clone();

        var renovacao = enviar(
            client,
            "POST",
            "/auth/refresh",
            null,
            obterCsrf(client),
            null
        );

        verificarStatus(renovacao, 200);

        String segundoJwt = JsonPath.read(
            renovacao.body(),
            "$.accessToken"
        );

        assertThat(segundoJwt).isNotEqualTo(primeiroJwt);

        assertThat(refreshCookie(cookies).getValue())
            .isNotEqualTo(cookieAntigo.getValue());

        // Simula um logout que ainda carrega o cookie anterior.
        cookies.getCookieStore().remove(
            baseUri(),
            refreshCookie(cookies)
        );
        cookies.getCookieStore().add(baseUri(), cookieAntigo);

        assertThat(refreshCookie(cookies).getValue())
            .isEqualTo(cookieAntigo.getValue());

        csrf = obterCsrf(client);

        // O JWT anterior à rotação ainda identifica a mesma sessão ativa.
        var logout = enviar(
            client,
            "POST",
            "/auth/logout",
            null,
            csrf,
            primeiroJwt
        );

        verificarStatus(logout, 204);

        Boolean revogada = jdbcTemplate.queryForObject("""
            SELECT revoked_at IS NOT NULL
            FROM sessoes_autenticacao
            WHERE id = ?
            """, Boolean.class, sessaoId);

        assertThat(revogada).isTrue();

        assertThat(logout.headers().allValues("set-cookie"))
            .anySatisfy(value -> assertThat(value)
                .startsWith("refresh_token=")
                .contains("Max-Age=0"));

        verificarStatus(enviar(
            client,
            "GET",
            "/usuarios/me",
            null,
            null,
            primeiroJwt
        ), 401);

        verificarStatus(enviar(
            client,
            "GET",
            "/usuarios/me",
            null,
            null,
            segundoJwt
        ), 401);
    }

    private HttpCookie refreshCookie(CookieManager cookies) {
        return cookies.getCookieStore().getCookies().stream()
            .filter(cookie -> cookie.getName().equals("refresh_token"))
            .findFirst()
            .orElseThrow();
    }

    private URI baseUri() {
        return URI.create("http://localhost:" + port);
    }

    private String obterCsrf(HttpClient client) throws Exception {
        var response = enviar(
            client,
            "GET",
            "/auth/csrf",
            null,
            null,
            null
        );

        verificarStatus(response, 200);

        return JsonPath.read(response.body(), "$.token");
    }

    private void verificarStatus(
        HttpResponse<String> response,
        int esperado
    ) {
        assertThat(response.statusCode())
            .as("Resposta HTTP: %s", response.body())
            .isEqualTo(esperado);
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
                baseUri() + "/api/v1" + endpoint
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
}