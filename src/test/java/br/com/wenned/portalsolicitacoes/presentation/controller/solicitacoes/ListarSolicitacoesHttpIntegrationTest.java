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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class ListarSolicitacoesHttpIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:16");

    private static final Instant DATA_BASE =
        Instant.parse("2026-10-05T06:00:00Z");

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
    void devePaginarSomenteSolicitacoesDoProprietario() throws Exception {
        var usuario = cadastrarEAutenticar();
        var outroUsuario = cadastrarEAutenticar();

        UUID antiga = inserirSolicitacao(
            usuario.id(), "TI", "ABERTO", DATA_BASE
        );
        UUID intermediaria = inserirSolicitacao(
            usuario.id(), "RH", "ABERTO", DATA_BASE.plusSeconds(1)
        );
        UUID recente = inserirSolicitacao(
            usuario.id(), "COMPRAS", "ABERTO", DATA_BASE.plusSeconds(2)
        );

        inserirSolicitacao(
            outroUsuario.id(), "TI", "ABERTO", DATA_BASE.plusSeconds(3)
        );

        var primeira = listar(usuario, "?page=0&size=2");
        verificarPagina(primeira, 0, 2, 3, 2);

        assertThat(ids(primeira))
            .containsExactly(recente.toString(), intermediaria.toString());

        List<String> proprietarios = JsonPath.read(
            primeira.body(), "$.content[*].solicitanteId"
        );

        assertThat(proprietarios).containsOnly(usuario.id().toString());

        var segunda = listar(usuario, "?page=1&size=2");
        verificarPagina(segunda, 1, 2, 3, 2);
        assertThat(ids(segunda)).containsExactly(antiga.toString());

        var foraDoIntervalo = listar(usuario, "?page=2&size=2");
        verificarPagina(foraDoIntervalo, 2, 2, 3, 2);
        assertThat(ids(foraDoIntervalo)).isEmpty();
    }

    @Test
    void deveDesempatarDatasIguaisPeloCodigoDecrescente() throws Exception {
        var usuario = cadastrarEAutenticar();

        UUID primeira = inserirSolicitacao(
            usuario.id(), "TI", "ABERTO", DATA_BASE
        );
        UUID segunda = inserirSolicitacao(
            usuario.id(), "TI", "ABERTO", DATA_BASE
        );

        var response = listar(usuario, "");

        verificarPagina(response, 0, 20, 2, 1);
        assertThat(ids(response))
            .containsExactly(segunda.toString(), primeira.toString());
    }

    @Test
    void deveAplicarFiltrosIndividuaisECombinados() throws Exception {
        var usuario = cadastrarEAutenticar();
        var outroUsuario = cadastrarEAutenticar();

        UUID tiAberta = inserirSolicitacao(
            usuario.id(), "TI", "ABERTO", DATA_BASE
        );
        UUID tiEmAtendimento = inserirSolicitacao(
            usuario.id(), "TI", "EM_ATENDIMENTO", DATA_BASE.plusSeconds(1)
        );
        UUID rhAberta = inserirSolicitacao(
            usuario.id(), "RH", "ABERTO", DATA_BASE.plusSeconds(2)
        );
        UUID tiConcluida = inserirSolicitacao(
            usuario.id(), "TI", "CONCLUIDO", DATA_BASE.plusSeconds(3)
        );

        inserirSolicitacao(
            outroUsuario.id(), "TI", "ABERTO", DATA_BASE.plusSeconds(4)
        );

        var porStatus = listar(usuario, "?status=ABERTO");
        verificarPagina(porStatus, 0, 20, 2, 1);
        assertThat(ids(porStatus))
            .containsExactly(rhAberta.toString(), tiAberta.toString());

        var porCategoria = listar(usuario, "?categoria=TI");
        verificarPagina(porCategoria, 0, 20, 3, 1);
        assertThat(ids(porCategoria)).containsExactly(
            tiConcluida.toString(),
            tiEmAtendimento.toString(),
            tiAberta.toString()
        );

        var combinados = listar(usuario, "?status=ABERTO&categoria=TI");
        verificarPagina(combinados, 0, 20, 1, 1);
        assertThat(ids(combinados)).containsExactly(tiAberta.toString());

        var semCorrespondencia = listar(
            usuario, "?status=CONCLUIDO&categoria=RH"
        );
        verificarPagina(semCorrespondencia, 0, 20, 0, 0);
        assertThat(ids(semCorrespondencia)).isEmpty();
    }

    @Test
    void deveRetornarPaginaVaziaParaUsuarioSemSolicitacoes() throws Exception {
        var usuario = cadastrarEAutenticar();

        var response = listar(usuario, "");

        verificarPagina(response, 0, 20, 0, 0);
        assertThat(ids(response)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "?page=-1",
        "?size=0",
        "?size=101",
        "?page=abc",
        "?status=INVALIDO",
        "?categoria=INVALIDA"
    })
    void deveRecusarParametrosInvalidos(String query) throws Exception {
        var usuario = cadastrarEAutenticar();

        var response = listar(usuario, query);

        assertThat(response.statusCode())
            .as("Resposta para %s: %s", query, response.body())
            .isEqualTo(400);
    }

    @Test
    void deveRecusarListagemSemAutenticacao() throws Exception {
        var response = enviar(
            novoCliente(), "GET", "/solicitacoes", null, null, null
        );

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void deveBuscarTituloParcialIgnorandoCaixaEEspacosExternos()
        throws Exception {

        var usuario = cadastrarEAutenticar();

        UUID esperado = inserirSolicitacao(
            usuario.id(), "TI", "ABERTO", DATA_BASE,
            "Troca de COMPUTADOR"
        );

        inserirSolicitacao(
            usuario.id(), "TI", "ABERTO", DATA_BASE.plusSeconds(1),
            "Troca de monitor"
        );

        var response = listar(
            usuario, "?titulo=" + codificar("  computador  ")
        );

        verificarPagina(response, 0, 20, 1, 1);
        assertThat(ids(response)).containsExactly(esperado.toString());

        var tituloVazio = listar(
            usuario, "?titulo=" + codificar("   ")
        );

        verificarPagina(tituloVazio, 0, 20, 2, 1);
    }

    @Test
    void deveTratarPercentualESublinhadoComoTextoLiteral() throws Exception {
        var usuario = cadastrarEAutenticar();

        UUID esperado = inserirSolicitacao(
            usuario.id(), "TI", "ABERTO", DATA_BASE,
            "Ajustar relatório 50%_final"
        );

        inserirSolicitacao(
            usuario.id(), "TI", "ABERTO", DATA_BASE.plusSeconds(1),
            "Ajustar relatório 50XYZfinal"
        );

        var response = listar(
            usuario, "?titulo=" + codificar("50%_final")
        );

        verificarPagina(response, 0, 20, 1, 1);
        assertThat(ids(response)).containsExactly(esperado.toString());
    }

    @Test
    void deveAplicarLimitesDoDiaNoFusoSaoPaulo() throws Exception {
        var usuario = cadastrarEAutenticar();

        // Em outubro de 2026, o dia local começa às 03:00 UTC.
        Instant inicio = Instant.parse("2026-10-05T03:00:00Z");
        Instant diaSeguinte = Instant.parse("2026-10-06T03:00:00Z");

        UUID antes = inserirSolicitacao(
            usuario.id(), "TI", "ABERTO", inicio.minusSeconds(1)
        );
        UUID noInicio = inserirSolicitacao(
            usuario.id(), "TI", "ABERTO", inicio
        );
        UUID noFinal = inserirSolicitacao(
            usuario.id(), "TI", "ABERTO", diaSeguinte.minusSeconds(1)
        );
        UUID depois = inserirSolicitacao(
            usuario.id(), "TI", "ABERTO", diaSeguinte
        );

        var periodoCompleto = listar(
            usuario, "?dataInicial=2026-10-05&dataFinal=2026-10-05"
        );

        verificarPagina(periodoCompleto, 0, 20, 2, 1);
        assertThat(ids(periodoCompleto)).containsExactly(
            noFinal.toString(), noInicio.toString()
        );

        var somenteInicio = listar(
            usuario, "?dataInicial=2026-10-05"
        );

        verificarPagina(somenteInicio, 0, 20, 3, 1);
        assertThat(ids(somenteInicio)).containsExactly(
            depois.toString(), noFinal.toString(), noInicio.toString()
        );

        var somenteFinal = listar(
            usuario, "?dataFinal=2026-10-05"
        );

        verificarPagina(somenteFinal, 0, 20, 3, 1);
        assertThat(ids(somenteFinal)).containsExactly(
            noFinal.toString(), noInicio.toString(), antes.toString()
        );
    }

    @Test
    void deveCombinarTodosOsFiltrosSemExporOutroProprietario()
        throws Exception {

        var usuario = cadastrarEAutenticar();
        var outroUsuario = cadastrarEAutenticar();

        UUID esperado = inserirSolicitacao(
            usuario.id(), "TI", "ABERTO", DATA_BASE,
            "Troca de computador"
        );

        inserirSolicitacao(
            outroUsuario.id(), "TI", "ABERTO", DATA_BASE,
            "Troca de computador"
        );
        inserirSolicitacao(
            usuario.id(), "RH", "ABERTO", DATA_BASE,
            "Troca de computador"
        );
        inserirSolicitacao(
            usuario.id(), "TI", "CONCLUIDO", DATA_BASE,
            "Troca de computador"
        );
        inserirSolicitacao(
            usuario.id(), "TI", "ABERTO", DATA_BASE,
            "Troca de monitor"
        );
        inserirSolicitacao(
            usuario.id(), "TI", "ABERTO",
            Instant.parse("2026-10-04T06:00:00Z"),
            "Troca de computador"
        );

        var response = listar(
            usuario,
            "?titulo=computador&categoria=TI&status=ABERTO"
                + "&dataInicial=2026-10-05&dataFinal=2026-10-05"
                + "&page=0&size=1"
        );

        verificarPagina(response, 0, 1, 1, 1);
        assertThat(ids(response)).containsExactly(esperado.toString());

        List<String> proprietarios = JsonPath.read(
            response.body(), "$.content[*].solicitanteId"
        );

        assertThat(proprietarios).containsOnly(usuario.id().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "?dataInicial=2026-10-06&dataFinal=2026-10-05",
        "?dataInicial=nao-e-data",
        "?dataFinal=2026-13-01"
    })
    void deveRecusarPeriodoInvalido(String query) throws Exception {
        var usuario = cadastrarEAutenticar();

        var response = listar(usuario, query);

        assertThat(response.statusCode())
            .as("Resposta para %s: %s", query, response.body())
            .isEqualTo(400);
    }

    private UUID inserirSolicitacao(
        UUID usuarioId,
        String categoria,
        String status,
        Instant data
    ) {
        return inserirSolicitacao(
            usuarioId, categoria, status, data, "Solicitação de teste"
        );
    }

    private UUID inserirSolicitacao(
        UUID usuarioId,
        String categoria,
        String status,
        Instant data,
        String titulo
    ) {
        UUID id = UUID.randomUUID();
        OffsetDateTime timestamp = data.atOffset(ZoneOffset.UTC);

        jdbcTemplate.update("""
            INSERT INTO solicitacoes (
                id, titulo, descricao, categoria, status,
                solicitante_id, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            id,
            titulo,
            "Descrição da solicitação de teste.",
            categoria,
            status,
            usuarioId,
            timestamp,
            timestamp
        );

        return id;
    }

    private HttpResponse<String> listar(
        UsuarioAutenticado usuario,
        String query
    ) throws Exception {
        return enviar(
            usuario.client(),
            "GET",
            "/solicitacoes" + query,
            null,
            null,
            usuario.jwt()
        );
    }

    private void verificarPagina(
        HttpResponse<String> response,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
        assertThat(response.statusCode())
            .as("Resposta da listagem: %s", response.body())
            .isEqualTo(200);

        Number actualPage = JsonPath.read(response.body(), "$.page");
        Number actualSize = JsonPath.read(response.body(), "$.size");
        Number actualTotal = JsonPath.read(
            response.body(), "$.totalElements"
        );
        Number actualPages = JsonPath.read(response.body(), "$.totalPages");

        assertThat(actualPage.intValue()).isEqualTo(page);
        assertThat(actualSize.intValue()).isEqualTo(size);
        assertThat(actualTotal.longValue()).isEqualTo(totalElements);
        assertThat(actualPages.intValue()).isEqualTo(totalPages);
    }

    private List<String> ids(HttpResponse<String> response) {
        return JsonPath.read(response.body(), "$.content[*].id");
    }

    private static String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    private UsuarioAutenticado cadastrarEAutenticar() throws Exception {
        HttpClient client = novoCliente();
        String email = "listagem-" + UUID.randomUUID() + "@example.com";

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

        UUID id = UUID.fromString(
            JsonPath.read(cadastro.body(), "$.id")
        );

        var csrfResponse = enviar(
            client, "GET", "/auth/csrf", null, null, null
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