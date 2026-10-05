package br.com.wenned.portalsolicitacoes.presentation.controller.solicitacoes;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
class OperacoesSolicitacaoHttpIntegrationTest {

    private static final String EDICAO = """
        {
          "titulo": "  Título atualizado  ",
          "descricao": "  Descrição atualizada  ",
          "categoria": "RH"
        }
        """;

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
    void deveEditarSolicitacaoAbertaPreservandoProprietarioECodigo()
        throws Exception {

        var usuario = cadastrarEAutenticar();
        UUID id = criarSolicitacao(usuario);

        var antes = consultar(usuario, id);
        String createdAt = JsonPath.read(antes.body(), "$.createdAt");
        Number codigo = JsonPath.read(antes.body(), "$.codigo");

        var response = operar(usuario, id, "PUT", "", EDICAO);

        verificarStatus(response, 204);
        assertThat(response.body()).isEmpty();

        var depois = consultar(usuario, id);
        verificarStatus(depois, 200);

        String titulo = JsonPath.read(depois.body(), "$.titulo");
        String descricao = JsonPath.read(depois.body(), "$.descricao");
        String categoria = JsonPath.read(depois.body(), "$.categoria");
        String status = JsonPath.read(depois.body(), "$.status");
        String proprietario = JsonPath.read(
            depois.body(),
            "$.solicitanteId"
        );
        String novaDataCriacao = JsonPath.read(
            depois.body(),
            "$.createdAt"
        );
        Number novoCodigo = JsonPath.read(depois.body(), "$.codigo");

        assertThat(titulo).isEqualTo("Título atualizado");
        assertThat(descricao).isEqualTo("Descrição atualizada");
        assertThat(categoria).isEqualTo("RH");
        assertThat(status).isEqualTo("ABERTO");
        assertThat(proprietario).isEqualTo(usuario.id().toString());
        assertThat(novaDataCriacao).isEqualTo(createdAt);
        assertThat(novoCodigo.longValue()).isEqualTo(codigo.longValue());
        assertThat(versao(id)).isEqualTo(1);
    }

    @Test
    void deveExcluirFisicamenteSolicitacaoAberta() throws Exception {
        var usuario = cadastrarEAutenticar();
        UUID id = criarSolicitacao(usuario);

        verificarStatus(operar(usuario, id, "DELETE", "", null), 204);

        Long quantidade = jdbcTemplate.queryForObject("""
            SELECT count(*) FROM solicitacoes WHERE id = ?
            """, Long.class, id);

        assertThat(quantidade).isZero();
        verificarStatus(consultar(usuario, id), 404);

        // O registro já não existe.
        verificarStatus(operar(usuario, id, "DELETE", "", null), 404);
    }

    @Test
    void deveAvancarStatusEBloquearEdicaoExclusaoETransicoesInvalidas()
        throws Exception {

        var usuario = cadastrarEAutenticar();
        UUID id = criarSolicitacao(usuario);

        // Não permite pular diretamente para CONCLUIDO.
        verificarConflito(alterarStatus(usuario, id, "CONCLUIDO"));
        assertThat(versao(id)).isZero();
        assertThat(statusPersistido(id)).isEqualTo("ABERTO");

        verificarStatus(alterarStatus(usuario, id, "EM_ATENDIMENTO"), 204);
        assertThat(statusPersistido(id)).isEqualTo("EM_ATENDIMENTO");
        assertThat(versao(id)).isEqualTo(1);

        verificarConflito(operar(usuario, id, "PUT", "", EDICAO));
        verificarConflito(operar(usuario, id, "DELETE", "", null));
        verificarConflito(alterarStatus(usuario, id, "ABERTO"));
        verificarConflito(alterarStatus(usuario, id, "EM_ATENDIMENTO"));

        assertThat(versao(id)).isEqualTo(1);

        verificarStatus(alterarStatus(usuario, id, "CONCLUIDO"), 204);
        assertThat(statusPersistido(id)).isEqualTo("CONCLUIDO");
        assertThat(versao(id)).isEqualTo(2);

        verificarConflito(operar(usuario, id, "PUT", "", EDICAO));
        verificarConflito(operar(usuario, id, "DELETE", "", null));
        verificarConflito(alterarStatus(usuario, id, "ABERTO"));
        verificarConflito(alterarStatus(usuario, id, "EM_ATENDIMENTO"));
        verificarConflito(alterarStatus(usuario, id, "CONCLUIDO"));

        assertThat(versao(id)).isEqualTo(2);
        verificarStatus(consultar(usuario, id), 200);
    }

    @ParameterizedTest
    @CsvSource({
        "PUT, EDITAR",
        "DELETE, EXCLUIR",
        "PATCH, STATUS"
    })
    void deveRecusarOperacaoSobreSolicitacaoDeOutroUsuario(
        String method,
        String operacao
    ) throws Exception {
        var proprietario = cadastrarEAutenticar();
        UUID id = criarSolicitacao(proprietario);
        var outroUsuario = cadastrarEAutenticar();

        var response = operar(
            outroUsuario,
            id,
            method,
            sufixo(operacao),
            corpo(operacao)
        );

        verificarStatus(response, 404);

        String code = JsonPath.read(response.body(), "$.code");
        assertThat(code).isEqualTo("REQUEST_NOT_FOUND");

        verificarStatus(consultar(proprietario, id), 200);
        assertThat(statusPersistido(id)).isEqualTo("ABERTO");
        assertThat(versao(id)).isZero();

        String titulo = jdbcTemplate.queryForObject("""
            SELECT titulo FROM solicitacoes WHERE id = ?
            """, String.class, id);

        assertThat(titulo).isEqualTo("Título original");
    }

    @ParameterizedTest
    @CsvSource({
        "PUT, EDITAR",
        "DELETE, EXCLUIR",
        "PATCH, STATUS"
    })
    void deveRetornar404ParaRegistroInexistente(
        String method,
        String operacao
    ) throws Exception {
        var usuario = cadastrarEAutenticar();

        verificarStatus(operar(
            usuario,
            UUID.randomUUID(),
            method,
            sufixo(operacao),
            corpo(operacao)
        ), 404);
    }

    @ParameterizedTest
    @CsvSource({
        "PUT, EDITAR",
        "DELETE, EXCLUIR",
        "PATCH, STATUS"
    })
    void deveRecusarOperacaoSemAutenticacao(
        String method,
        String operacao
    ) throws Exception {
        var response = enviar(
            novoCliente(),
            method,
            "/solicitacoes/" + UUID.randomUUID() + sufixo(operacao),
            corpo(operacao),
            null,
            null
        );

        verificarStatus(response, 401);
    }

    @Test
    void deveRecusarEdicaoInvalidaSemAlterarRegistro() throws Exception {
        var usuario = cadastrarEAutenticar();
        UUID id = criarSolicitacao(usuario);

        var response = operar(
            usuario,
            id,
            "PUT",
            "",
            """
            {"titulo":" ","descricao":"Descrição","categoria":"TI"}
            """
        );

        verificarStatus(response, 400);
        assertThat(versao(id)).isZero();
    }

    @Test
    void deveRecusarStatusAusenteOuDesconhecido() throws Exception {
        var usuario = cadastrarEAutenticar();
        UUID id = criarSolicitacao(usuario);

        verificarStatus(
            operar(usuario, id, "PATCH", "/status", "{}"),
            400
        );

        verificarStatus(
            operar(
                usuario,
                id,
                "PATCH",
                "/status",
                "{\"status\":\"INVALIDO\"}"
            ),
            400
        );

        assertThat(statusPersistido(id)).isEqualTo("ABERTO");
        assertThat(versao(id)).isZero();
    }

    private String sufixo(String operacao) {
        return operacao.equals("STATUS") ? "/status" : "";
    }

    private String corpo(String operacao) {
        return switch (operacao) {
            case "EDITAR" -> EDICAO;
            case "STATUS" -> "{\"status\":\"EM_ATENDIMENTO\"}";
            default -> null;
        };
    }

    private HttpResponse<String> alterarStatus(
        UsuarioAutenticado usuario,
        UUID id,
        String status
    ) throws Exception {
        return operar(
            usuario,
            id,
            "PATCH",
            "/status",
            "{\"status\":\"%s\"}".formatted(status)
        );
    }

    private HttpResponse<String> operar(
        UsuarioAutenticado usuario,
        UUID id,
        String method,
        String sufixo,
        String body
    ) throws Exception {
        return enviar(
            usuario.client(),
            method,
            "/solicitacoes/" + id + sufixo,
            body,
            null,
            usuario.jwt()
        );
    }

    private HttpResponse<String> consultar(
        UsuarioAutenticado usuario,
        UUID id
    ) throws Exception {
        return operar(usuario, id, "GET", "", null);
    }

    private long versao(UUID id) {
        return jdbcTemplate.queryForObject("""
            SELECT version FROM solicitacoes WHERE id = ?
            """, Long.class, id);
    }

    private String statusPersistido(UUID id) {
        return jdbcTemplate.queryForObject("""
            SELECT status FROM solicitacoes WHERE id = ?
            """, String.class, id);
    }

    private void verificarStatus(
        HttpResponse<String> response,
        int esperado
    ) {
        assertThat(response.statusCode())
            .as("Resposta HTTP: %s", response.body())
            .isEqualTo(esperado);
    }

    private void verificarConflito(HttpResponse<String> response) {
        verificarStatus(response, 409);

        String code = JsonPath.read(response.body(), "$.code");
        assertThat(code).isEqualTo("REQUEST_STATE_CONFLICT");
    }

    private UUID criarSolicitacao(UsuarioAutenticado usuario)
        throws Exception {

        var response = enviar(
            usuario.client(),
            "POST",
            "/solicitacoes",
            """
            {
              "titulo":"Título original",
              "descricao":"Descrição original",
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
        String email = "operacoes-" + UUID.randomUUID() + "@example.com";

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