package br.com.wenned.portalsolicitacoes.infrastructure.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    public OpenAPI portalOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Portal de Solicitações Internas")
                .version("1.0.0")
                .description("""
                    API para cadastro, autenticação, solicitações e dashboard.

                    Os endpoints protegidos exigem access token Bearer JWT.
                    A sessão associada ao JWT também é validada no banco.

                    Login, renovação e logout exigem X-CSRF-TOKEN.
                    Obtenha o token em GET /api/v1/auth/csrf e preserve
                    os cookies recebidos pelo navegador.

                    O refresh token é enviado somente por cookie HttpOnly.
                    Sua rotação não estende a validade absoluta da sessão.
                    """))
            .components(new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "Informe o access token retornado pelo login."
                        )
                )
                .addSecuritySchemes(
                    "csrfToken",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-CSRF-TOKEN")
                        .description("""
                            Informe o campo token retornado por
                            GET /api/v1/auth/csrf.
                            O cookie correspondente deve ser preservado.
                            """)
                )
                .addSecuritySchemes(
                    "refreshCookie",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("refresh_token")
                        .description("""
                            Cookie HttpOnly definido pelo login e renovação.
                            O navegador o envia automaticamente.
                            Não deve ser preenchido manualmente no Swagger.
                            """)
                ))
            .addSecurityItem(
                new SecurityRequirement().addList("bearerAuth")
            );
    }

    @Bean
    public OpenApiCustomizer portalContractCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    ApiResponses responses = operation.getResponses();

                    if (responses == null) {
                        responses = new ApiResponses();
                        operation.setResponses(responses);
                    }

                    boolean cadastro =
                        path.equals("/api/v1/usuarios")
                            && method == PathItem.HttpMethod.POST;

                    boolean csrf =
                        path.equals("/api/v1/auth/csrf")
                            && method == PathItem.HttpMethod.GET;

                    boolean login =
                        path.equals("/api/v1/auth/login")
                            && method == PathItem.HttpMethod.POST;

                    boolean refresh =
                        path.equals("/api/v1/auth/refresh")
                            && method == PathItem.HttpMethod.POST;

                    boolean logout =
                        path.equals("/api/v1/auth/logout")
                            && method == PathItem.HttpMethod.POST;

                    boolean operacaoSolicitacao =
                        path.equals("/api/v1/solicitacoes/{id}")
                            && (method == PathItem.HttpMethod.PUT
                                || method == PathItem.HttpMethod.DELETE);

                    boolean alteracaoStatus =
                        path.equals("/api/v1/solicitacoes/{id}/status")
                            && method == PathItem.HttpMethod.PATCH;

                    if (cadastro || csrf) {
                        operation.setSecurity(List.of());
                    } else if (login || logout) {
                        operation.setSecurity(List.of(
                            new SecurityRequirement().addList("csrfToken")
                        ));
                    } else if (refresh) {
                        operation.setSecurity(List.of(
                            new SecurityRequirement()
                                .addList("csrfToken")
                                .addList("refreshCookie")
                        ));
                    }

                    if (cadastro) {
                        definirSucesso(responses, "201", "Usuário cadastrado.");
                        responses.addApiResponse(
                            "409",
                            resposta("E-mail já cadastrado.")
                        );
                    }

                    if (path.equals("/api/v1/solicitacoes")
                        && method == PathItem.HttpMethod.POST) {
                        definirSucesso(
                            responses,
                            "201",
                            "Solicitação criada."
                        );
                    }

                    if (logout || operacaoSolicitacao || alteracaoStatus) {
                        responses.remove("200");
                        responses.addApiResponse(
                            "204",
                            resposta("Operação concluída, sem corpo.")
                        );
                    }

                    if (!cadastro && !csrf) {
                        responses.addApiResponse(
                            "401",
                            resposta("Autenticação ou credenciais inválidas.")
                        );
                    }

                    if (login || refresh || logout) {
                        responses.addApiResponse(
                            "403",
                            resposta("Token CSRF ausente ou inválido.")
                        );
                    }

                    boolean consultaIndividual =
                        path.equals("/api/v1/solicitacoes/{id}")
                            && method == PathItem.HttpMethod.GET;

                    if (consultaIndividual
                        || operacaoSolicitacao
                        || alteracaoStatus) {
                        responses.addApiResponse(
                            "404",
                            resposta(
                                "Solicitação inexistente ou pertencente a outro usuário."
                            )
                        );
                    }

                    if (operacaoSolicitacao || alteracaoStatus) {
                        responses.addApiResponse(
                            "409",
                            resposta(
                                "Estado incompatível ou conflito de escrita."
                            )
                        );
                    }

                    if (!csrf
                        && !logout
                        && !path.equals("/api/v1/dashboard")
                        && !path.equals("/api/v1/usuarios/me")) {
                        responses.addApiResponse(
                            "400",
                            resposta("Dados, parâmetros ou JSON inválidos.")
                        );
                    }
                })
            );
        };
    }

    private static void definirSucesso(
        ApiResponses responses,
        String code,
        String description
    ) {
        ApiResponse sucesso = responses.remove("200");

        if (sucesso == null) {
            sucesso = responses.get(code);
        }

        if (sucesso == null) {
            sucesso = new ApiResponse();
        }

        sucesso.setDescription(description);
        responses.addApiResponse(code, sucesso);
    }

    private static ApiResponse resposta(String description) {
        return new ApiResponse().description(description);
    }
}