package br.com.wenned.portalsolicitacoes.presentation.controller.usuarios;

import org.springframework.test.context.ActiveProfiles;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

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
class CadastroUsuarioIntegrationTest {

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

    @Autowired
    PasswordEncoder passwordEncoder;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Test
    void deveCadastrarNormalizarPersistirEProtegerSenha() throws Exception {
        String email = uniqueEmail();
        String password = "Teste12345!";

        var response = post("""
                {
                  "name": " Ana ",
                  "email": " %s ",
                  "password": "%s"
                }
                """.formatted(email.toUpperCase(java.util.Locale.ROOT), password));

        assertThat(response.statusCode()).isEqualTo(201);

        var json = JsonPath.parse(response.body());

        String id = json.read("$.id");
        String name = json.read("$.name");
        String returnedEmail = json.read("$.email");
        String createdAt = json.read("$.createdAt");

        assertThat(UUID.fromString(id)).isNotNull();
        assertThat(name).isEqualTo("Ana");
        assertThat(returnedEmail).isEqualTo(email);
        assertThat(createdAt).isNotBlank();

        assertThat(response.body())
                .doesNotContain("password", "passwordHash", "password_hash");

        String storedHash = jdbcTemplate.queryForObject("""
                SELECT password_hash
                FROM usuarios
                WHERE id = ?
                """, String.class, UUID.fromString(id));

        assertThat(storedHash).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, storedHash)).isTrue();

        String storedName = jdbcTemplate.queryForObject("""
                SELECT name FROM usuarios WHERE id = ?
                """, String.class, UUID.fromString(id));

        assertThat(storedName).isEqualTo("Ana");
    }

    @Test
    void deveRecusarEmailDuplicadoMesmoComMaiusculas() throws Exception {
        String email = uniqueEmail();

        var first = post("""
                {
                  "name": "Ana",
                  "email": "%s",
                  "password": "Teste12345!"
                }
                """.formatted(email));

        assertThat(first.statusCode()).isEqualTo(201);

        var duplicate = post("""
                {
                  "name": "Outro Usuario",
                  "email": "%s",
                  "password": "Outra12345!"
                }
                """.formatted(email.toUpperCase(java.util.Locale.ROOT)));

        assertThat(duplicate.statusCode()).isEqualTo(409);

        String code = JsonPath.read(duplicate.body(), "$.code");
        assertThat(code).isEqualTo("EMAIL_ALREADY_REGISTERED");

        Long count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM usuarios WHERE email = ?
                """, Long.class, email);

        assertThat(count).isEqualTo(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"name":"An","email":"%s","password":"Teste12345!"}
            """,
            """
            {"name":"Ana","email":"%s","password":"1234567"}
            """,
            """
            {"name":"Ana","email":"%s"}
            """
    })
    void deveRecusarDadosInvalidosSemPersistir(String template)
            throws Exception {

        String email = uniqueEmail();

        var response = post(template.formatted(email));

        assertThat(response.statusCode()).isEqualTo(400);

        String code = JsonPath.read(response.body(), "$.code");
        assertThat(code).isEqualTo("VALIDATION_ERROR");

        Long count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM usuarios WHERE email = ?
                """, Long.class, email);

        assertThat(count).isZero();
    }

    @Test
    void deveRecusarJsonMalformado() throws Exception {
        var response = post("{");

        assertThat(response.statusCode()).isEqualTo(400);

        String code = JsonPath.read(response.body(), "$.code");
        assertThat(code).isEqualTo("INVALID_REQUEST_BODY");
    }

    private HttpResponse<String> post(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + port + "/api/v1/usuarios"
                ))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private String uniqueEmail() {
        return "cadastro-" + UUID.randomUUID() + "@example.com";
    }
}