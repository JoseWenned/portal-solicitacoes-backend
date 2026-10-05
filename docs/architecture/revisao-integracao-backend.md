# Revisão da integração do backend

## Objetivo

Preparar o contrato de autenticação e comunicação
para a integração com o frontend.

## Problema identificado

Após a rotação, o hash do refresh token anterior deixa
de identificar a sessão.

Um logout somente com esse cookie antigo retorna 204,
mas pode não revogar a sessão.

## Ajuste implementado

O logout aceita opcionalmente um Bearer JWT válido.

EncerrarSessaoAutenticadaUseCase consulta a sessão indicada
pela claim sid, verifica seu proprietário e a revoga.

O fluxo existente de logout por refresh cookie é preservado.
Se JWT e cookie identificarem sessões diferentes,
ambas são revogadas.

A proteção CSRF permanece obrigatória.
O identificador da sessão não é recebido diretamente do cliente
por query ou corpo.

## Limites

Logout com JWT válido identifica a sessão mesmo após rotação.

Logout somente com refresh cookie antigo continua sujeito
à limitação de identificação após rotação.

Bearer inválido ou expirado é rejeitado pelo filtro com 401.
Chamadas somente por cookie devem omitir Authorization.

A coordenação das requisições no frontend permanece necessária.
Uma resposta de renovação atrasada pode chegar após o logout,
mas seus tokens não devem autorizar acesso à sessão já revogada.

## Contrato previsto para o frontend

- Access token mantido em memória.
- API acessada por /api na mesma origem.
- Proxy do Vite no desenvolvimento.
- Proxy do Nginx no ambiente Docker.
- Cookies preservados nas requisições.
- CSRF obtido pelo endpoint antes das operações de autenticação.
- Somente uma renovação em andamento por instância da aplicação.
- Logout aguarda a renovação em andamento antes de enviar a requisição.
- Logout envia Bearer válido quando disponível.
- Respostas atrasadas não restauram o estado após encerramento local.
- Falhas de renovação não entram em repetição infinita.

Essas regras do frontend ainda não foram implementadas.

## Configuração conferida

- Chave JWT fornecida somente ao backend pelo Compose.
- PostgreSQL com volume persistente e healthcheck.
- Portas publicadas em 127.0.0.1.
- Cookies HttpOnly e SameSite=Lax.
- Refresh cookie com Path=/api/v1/auth.
- Secure configurável para HTTPS.
- .env.example sem chave JWT real.

## Validação confirmada

Resultados informados pelo desenvolvedor no Ubuntu/WSL:

- Teste de logout após rotação aprovado.
- Cookie antigo restaurado deliberadamente antes do logout.
- Sessão identificada pelo JWT válido e revogada no PostgreSQL.
- Remoção do refresh cookie verificada.
- Ambos os access tokens rejeitados após logout.
- Suíte completa com ./mvnw verify: 146 testes.
- Nenhuma falha, erro ou teste ignorado.
- BUILD SUCCESS.
- CI desta branch: pendente.

O cenário foi reproduzido sequencialmente, sem múltiplas threads.
Logout somente com cookie antigo continua exigindo
a coordenação de renovação e logout no frontend.

## Teste de regressão do logout

LogoutAposRotacaoHttpIntegrationTest reproduz:

1. Login e captura do refresh cookie original.
2. Renovação com troca do cookie.
3. Restauração deliberada do cookie anterior.
4. Logout com cookie antigo, CSRF e JWT válido.
5. Verificação da revogação no PostgreSQL.
6. Rejeição dos JWTs emitidos antes e depois da rotação.

O teste reproduz o cenário sequencialmente.
Não executa requisições concorrentes.

## Evidência anterior

A suíte permaneceu aprovada com 145 testes após o ajuste
do controller e a inclusão do caso de uso de encerramento.

Execução do teste de regressão: pendente.