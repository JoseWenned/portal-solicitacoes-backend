# Autenticação JWT

## Estratégia

- Access token JWT com validade máxima de 15 minutos.
- Access token será mantido em memória no React.
- Refresh token aleatório enviado em cookie HttpOnly.
- Sessão com validade absoluta de oito horas.
- Hash do refresh token armazenado no PostgreSQL.
- Rotação sem extensão da validade absoluta.
- Logout com revogação da sessão.
- Verificação da sessão associada ao JWT nos acessos protegidos.

A solução mantém estado no banco para permitir revogação imediata.

O backend possui integração HTTP implementada.
A integração com o frontend ainda não foi desenvolvida.

## Organização

O código segue a organização por camadas:
domain, application, infrastructure e presentation.

Modelos de persistência ficam separados das entidades de domínio.
Mappers específicos realizam a conversão entre essas representações.

## Domínio

SessaoAutenticacao representa um login realizado.

Possui identificador, usuário, hash do refresh token,
datas de criação, expiração e revogação, além da versão persistida.

A sessão fica inativa no instante da expiração ou após a revogação.

Rotação e revogação preservam a identidade.
A rotação não altera a expiração absoluta.

O domínio não depende de Spring, JWT ou JPA.

## Aplicação

Os contratos permitem:

- Consultar usuário por e-mail e identificador.
- Verificar senha pelo PasswordHasher.
- Criar e consultar sessões.
- Rotacionar refresh token de forma condicionada.
- Revogar sessões.
- Gerar access token e refresh token.

AutenticacaoUseCase coordena login, renovação e logout.

No login, verifica a senha e cria a sessão.
Quando o usuário não existe, compara a senha com um hash BCrypt
auxiliar para reduzir diferenças de processamento.

Na renovação, exige sessão ativa e substitui o refresh token
por uma operação atômica.

A expiração do JWT é limitada pela expiração da sessão.

AutenticacaoResultado transporta internamente tokens e dados públicos.
O controller retorna o refresh token somente pelo cookie.

ConsultarUsuarioAutenticadoUseCase consulta os dados públicos
do usuário identificado pela autenticação.

## Persistência

- SessaoAutenticacaoModel mapeia a tabela.
- SessaoAutenticacaoMapper converte domínio e modelo.
- SessaoAutenticacaoRepositorioJPA define consultas e atualizações.
- SessaoAutenticacaoRepositoryAdapter implementa a porta.

A criação utiliza persist para inserir uma sessão nova.

A rotação verifica versão, hash anterior e estado ativo em um único
UPDATE, incrementando a versão sem alterar a expiração absoluta.

A revogação altera somente sessões ainda não revogadas,
preservando a primeira data de encerramento.

Não há operação genérica de atualização da sessão.

## Provedores de tokens

SecureRefreshTokenProvider gera 32 bytes aleatórios,
codificados em Base64 URL sem padding.

O hash SHA-256 do refresh token é armazenado no banco.

JwtAccessTokenProvider emite JWT assinado com HS256.

As claims identificam usuário, sessão, emissor, destinatário,
identificador do token e datas de validade.

Senha, hash de senha e refresh token não são incluídos no JWT.

## Configuração

AutenticacaoConfiguration conecta os provedores e o caso de uso.

JWT_SECRET_BASE64 fornece a chave de assinatura externamente.
Após decodificação, a chave deve possuir pelo menos 32 bytes.

O Compose fornece essa variável ao serviço backend.

O perfil test utiliza uma chave pública exclusiva para testes.
As classes de integração ativam o perfil com ActiveProfiles.

O hash BCrypt auxiliar é criado uma vez na inicialização.

Validades configuradas:

- Access token: 15 minutos.
- Sessão: oito horas, sem extensão durante a renovação.

AUTH_COOKIE_SECURE controla o atributo Secure dos cookies.
No desenvolvimento HTTP local, utiliza false.
Na execução HTTPS, deve utilizar true.

## Segurança HTTP

HttpSecurityConfiguration configura a API como Resource Server.

A validação do JWT verifica:

- Assinatura HS256.
- Emissor.
- Audiência.
- Expiração.
- Identificadores de usuário e sessão.
- Sessão ativa e pertencente ao usuário informado.

A sessão HTTP do Spring está desabilitada.
A sessão de autenticação permanece armazenada no PostgreSQL.

Login, renovação e logout exigem token CSRF.
O token é obtido por GET /api/v1/auth/csrf e enviado
no cabeçalho X-CSRF-TOKEN, junto com o cookie correspondente.

O cadastro é público e não utiliza cookie para autorizar a operação.
Os demais recursos protegidos utilizam Authorization: Bearer.

## Cookies

O refresh token utiliza:

- HttpOnly.
- SameSite=Lax.
- Path=/api/v1/auth.
- Max-Age limitado pela validade restante da sessão.
- Secure configurável.

O cookie CSRF também utiliza HttpOnly.
Seu token é disponibilizado pelo endpoint específico.

Respostas com tokens utilizam Cache-Control: no-store.

## Endpoints implementados

| Método | Endpoint | Responsabilidade |
|---|---|---|
| GET | /api/v1/auth/csrf | Obter token CSRF |
| POST | /api/v1/auth/login | Autenticar e criar sessão |
| POST | /api/v1/auth/refresh | Renovar tokens |
| POST | /api/v1/auth/logout | Revogar sessão e remover cookie |
| GET | /api/v1/usuarios/me | Consultar usuário autenticado |

A presença dos endpoints não significa que todos os seus fluxos
já foram validados por testes HTTP.

## Testes aprovados

- Domínio da sessão: sete testes.
- Persistência da sessão: seis testes de integração.
- Provedores de tokens: quatro testes.
- Casos de uso de autenticação: dez testes.

A suíte completa contém também os testes anteriores
de cadastro e migrations.

Os casos de uso cobrem login, credenciais incorretas,
comparação auxiliar, renovação, expiração, revogação,
falha da atualização atômica e logout.

Os testes de persistência não simulam múltiplas threads concorrentes.

## Validação HTTP e suíte completa

- Testes HTTP de autenticação: 5 executados, sem falhas,
  erros ou testes ignorados.
- Fluxo de login, renovação e logout validado por HTTP.
- Rotação confirmada pela alteração do JWT e do hash
  do refresh token persistido.
- Logout confirmado pela revogação da sessão e rejeição
  dos access tokens emitidos anteriormente.
- Operações de autenticação sem CSRF retornam HTTP 403.
- Senha incorreta retorna HTTP 401 sem criar sessão.
- Suíte completa com ./mvnw verify: 60 testes, sem falhas,
  erros ou testes ignorados; BUILD SUCCESS.
- CI desta branch: pendente.

## Limitações e pendências

- Validar login, renovação e logout por HTTP.
- Verificar por HTTP a rejeição do JWT após revogação.
- Verificar por HTTP a rejeição de operações sem CSRF.
- Coordenar renovação e logout concorrentes.
- Unificar os campos das respostas de erro dos filtros e controllers.
- Configurar CORS se houver necessidade de origens distintas.
- Integrar o frontend.
- Executar a CI desta branch.

O logout identifica a sessão pelo refresh token atual.
O envio de um token antigo durante uma rotação concorrente
continua sendo uma situação a tratar.