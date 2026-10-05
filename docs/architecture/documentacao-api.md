# Documentação OpenAPI

## Objetivo

Disponibilizar o contrato da API e uma interface para consulta
e execução manual dos endpoints.

## Implementação

springdoc-openapi-starter-webmvc-ui 3.1.1.

OpenApiConfiguration define informações da API,
esquemas de segurança e ajustes dos códigos de resposta.

A geração considera os controllers de apresentação
e os caminhos /api/v1/**.

## Acesso

- /v3/api-docs: contrato em JSON.
- /swagger-ui/index.html: interface Swagger.
- /swagger-ui.html: entrada alternativa.

Os caminhos GET da documentação são públicos.

Essa liberação não altera a proteção dos endpoints de negócio,
a validação das sessões ou a exigência de CSRF.

## Segurança documentada

- bearerAuth: access token JWT.
- csrfToken: header X-CSRF-TOKEN.
- refreshCookie: cookie refresh_token.

Cadastro e obtenção de CSRF não exigem Bearer.

Login exige CSRF.
Renovação exige CSRF e refresh cookie.
Logout exige CSRF e revoga a sessão quando identificada pelo cookie.

Os demais endpoints exigem Bearer JWT.

## Uso da autenticação no Swagger

1. Execute GET /api/v1/auth/csrf.
2. Copie o campo token retornado.
3. Abra Authorize e preencha csrfToken.
4. Execute o login.
5. Copie accessToken e preencha bearerAuth.

O navegador preserva e envia os cookies HttpOnly.
Não é necessário preencher refreshCookie manualmente.

Se o cookie CSRF mudar, obtenha outro token e atualize csrfToken.

A autorização não é persistida após recarregar a interface.

## Limitações

Os schemas de sucesso são gerados a partir dos tipos de retorno.
As respostas de erro recebem códigos e descrições,
mas seus schemas ainda não foram detalhados nesta configuração.

A documentação é apoio ao contrato e não substitui
os testes HTTP ou as regras de segurança.

## Validação da documentação

Resultados informados pelo desenvolvedor:

- Contrato /v3/api-docs gerado em OpenAPI 3.1.0.
- Dez caminhos da API documentados.
- Swagger UI retorna HTTP 200 e abre no navegador.
- Dashboard sem JWT continua retornando HTTP 401.
- Respostas 201 de cadastro e criação conferidas no contrato.
- Respostas 204 de edição, exclusão, alteração de status
  e logout conferidas no contrato.
- Suíte completa após configurar OpenAPI: 145 testes,
  sem falhas, erros ou testes ignorados; BUILD SUCCESS.
- CI desta branch: pendente.