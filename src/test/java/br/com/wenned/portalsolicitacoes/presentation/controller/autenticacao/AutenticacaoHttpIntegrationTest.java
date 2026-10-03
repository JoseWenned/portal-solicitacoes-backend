package br.com.wenned.portalsolicitacoes.presentation.controller.autenticacao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class AutenticacaoHttpIntegrationTest {

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
    void deveFazerLoginRenovarESairInvalidandoOsJwts() throws Exception {
        HttpClient client = novoCliente();
        String email = "auth-" + UUID.randomUUID() + "@example.com";

        cadastrar(client, email);
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

        assertThat(login.statusCode())
            .as("Resposta do login: %s", login.body())
            .isEqualTo(200);

        assertThat(login.body()).doesNotContain(
            "refreshToken",
            "password",
            "passwordHash"
        );

        String cookie = login.headers()
            .allValues("set-cookie")
            .stream()
            .filter(value -> value.startsWith("refresh_token="))
            .findFirst()
            .orElseThrow();

        assertThat(cookie).contains(
            "HttpOnly",
            "SameSite=Lax",
            "Path=/api/v1/auth"
        );

        String primeiroJwt = JsonPath.read(login.body(), "$.accessToken");
        String tipo = JsonPath.read(login.body(), "$.tokenType");

        assertThat(tipo).isEqualTo("Bearer");

        UUID sessaoId = UUID.fromString(
            jwtDecoder.decode(primeiroJwt).getClaimAsString("sid")
        );

        String hashAnterior = hashDaSessao(sessaoId);

        var me = enviar(
            client,
            "GET",
            "/usuarios/me",
            null,
            null,
            primeiroJwt
        );

        assertThat(me.statusCode())
            .as("Resposta da consulta de usuário: %s", me.body())
            .isEqualTo(200);

        String returnedEmail = JsonPath.read(me.body(), "$.email");
        assertThat(returnedEmail).isEqualTo(email);

        csrf = obterCsrf(client);

        var refresh = enviar(
            client,
            "POST",
            "/auth/refresh",
            null,
            csrf,
            null
        );

        assertThat(refresh.statusCode())
            .as("Resposta do refresh: %s", refresh.body())
            .isEqualTo(200);

        String segundoJwt = JsonPath.read(refresh.body(), "$.accessToken");

        assertThat(segundoJwt).isNotEqualTo(primeiroJwt);
        assertThat(hashDaSessao(sessaoId)).isNotEqualTo(hashAnterior);

        var meAposRenovacao = enviar(
            client,
            "GET",
            "/usuarios/me",
            null,
            null,
            segundoJwt
        );

        assertThat(meAposRenovacao.statusCode())
            .as(
                "Resposta da consulta após renovação: %s",
                meAposRenovacao.body()
            )
            .isEqualTo(200);

        csrf = obterCsrf(client);

        var logout = enviar(
            client,
            "POST",
            "/auth/logout",
            null,
            csrf,
            null
        );

        assertThat(logout.statusCode())
            .as("Resposta do logout: %s", logout.body())
            .isEqualTo(204);

        assertThat(logout.headers().allValues("set-cookie"))
            .anySatisfy(value -> assertThat(value)
                .startsWith("refresh_token=")
                .contains("Max-Age=0"));

        Boolean revogada = jdbcTemplate.queryForObject("""
            SELECT revoked_at IS NOT NULL
            FROM sessoes_autenticacao
            WHERE id = ?
            """, Boolean.class, sessaoId);

        assertThat(revogada).isTrue();

        assertThat(enviar(
            client,
            "GET",
            "/usuarios/me",
            null,
            null,
            primeiroJwt
        ).statusCode()).isEqualTo(401);

        assertThat(enviar(
            client,
            "GET",
            "/usuarios/me",
            null,
            null,
            segundoJwt
        ).statusCode()).isEqualTo(401);

        csrf = obterCsrf(client);

        var refreshAposLogout = enviar(
            client,
            "POST",
            "/auth/refresh",
            null,
            csrf,
            null
        );

        assertThat(refreshAposLogout.statusCode())
            .as(
                "Resposta do refresh após logout: %s",
                refreshAposLogout.body()
            )
            .isEqualTo(401);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/auth/login",
        "/auth/refresh",
        "/auth/logout"
    })
    void deveRecusarOperacaoSemCsrf(String endpoint) throws Exception {
        var response = enviar(
            novoCliente(),
            "POST",
            endpoint,
            "{}",
            null,
            null
        );

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void deveRecusarCredenciaisIncorretasSemCriarSessao() throws Exception {
        HttpClient client = novoCliente();
        String email = "auth-" + UUID.randomUUID() + "@example.com";

        cadastrar(client, email);
        String csrf = obterCsrf(client);

        var response = enviar(
            client,
            "POST",
            "/auth/login",
            """
            {"email":"%s","password":"SenhaIncorreta!"}
            """.formatted(email),
            csrf,
            null
        );

        assertThat(response.statusCode())
            .as("Resposta para senha incorreta: %s", response.body())
            .isEqualTo(401);

        Long count = jdbcTemplate.queryForObject("""
            SELECT count(*)
            FROM sessoes_autenticacao s
            JOIN usuarios u ON u.id = s.usuario_id
            WHERE u.email = ?
            """, Long.class, email);

        assertThat(count).isZero();

        assertThat(response.headers().allValues("set-cookie"))
            .noneMatch(value -> value.startsWith("refresh_token="));
    }

    private void cadastrar(HttpClient client, String email) throws Exception {
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

        assertThat(response.statusCode())
            .as("Resposta da obtenção de CSRF: %s", response.body())
            .isEqualTo(200);

        return JsonPath.read(response.body(), "$.token");
    }

    private String hashDaSessao(UUID id) {
        return jdbcTemplate.queryForObject("""
            SELECT refresh_token_hash
            FROM sessoes_autenticacao
            WHERE id = ?
            """, String.class, id);
    }

    private HttpClient novoCliente() {
        CookieManager cookies = new CookieManager(
            null,
            CookiePolicy.ACCEPT_ALL
        );

        return HttpClient.newBuilder()
            .cookieHandler(cookies)
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
}