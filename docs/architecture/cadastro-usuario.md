# Arquitetura do cadastro de usuário

## Objetivo

Implementar POST /api/v1/usuarios com validação dos dados,
unicidade de e-mail e armazenamento protegido da senha.

## Organização

O backend é organizado por camadas na raiz do pacote:
domain, application, infrastructure e presentation.

Dentro das camadas, os arquivos são agrupados por responsabilidade
e conceito de negócio.

## Domínio

Usuario representa a entidade de domínio.

- Identidade definida por UUID.
- Nome com 3 a 100 pontos de código Unicode após remover
  espaços externos.
- E-mail normalizado para minúsculas e sem espaços externos.
- Criação e reconstituição controladas.
- Igualdade baseada no identificador.
- Sem setters, pois edição de usuários não faz parte do escopo.
- Sem dependências de Spring ou JPA.

EmailJaCadastradoException representa o conflito de e-mail.

## Aplicação

CadastrarUsuarioUseCase coordena o cadastro:

1. Normaliza e valida nome e e-mail.
2. Valida a senha.
3. Consulta a existência do e-mail.
4. Solicita a geração do hash.
5. Cria e persiste o usuário.
6. Retorna somente dados públicos.

UsuarioRepository define o contrato de persistência.
PasswordHasher define o contrato de geração do hash.

Clock é recebido como dependência para permitir controle
da data de criação nos testes.

## Infraestrutura

- UsuarioModel mapeia a tabela usuarios.
- UsuarioMapper converte entre domínio e modelo de persistência.
- UsuarioRepositorioJPA executa operações pelo Spring Data.
- UsuarioRepositoryAdapter implementa a porta de persistência.
- BCryptPasswordHasher implementa a proteção da senha.
- UsuarioConfiguration conecta as dependências.

A restrição uk_usuarios_email garante unicidade no banco.
O adaptador traduz sua violação para EmailJaCadastradoException.

A consulta prévia de e-mail não substitui essa restrição,
pois cadastros simultâneos podem ultrapassar a consulta inicial.

## Política de senha

- Mínimo de 8 pontos de código Unicode.
- Máximo de 72 bytes em UTF-8.
- Sem normalização ou remoção de espaços.
- Hash BCrypt com custo 12.

## Apresentação

UsuarioController recebe o cadastro e executa o caso de uso.

CadastrarUsuarioRequest representa a entrada e aplica
validações com Bean Validation.

UsuarioResponse retorna id, name, email e createdAt.
Não retorna senha ou hash.

ApiExceptionHandler padroniza as respostas de erro.

## Contrato HTTP

POST /api/v1/usuarios

Entrada: name, email e password.

Respostas:

- 201: usuário cadastrado.
- 400: dados inválidos ou corpo JSON inválido.
- 409: e-mail já cadastrado.
- 500: erro inesperado, sem detalhes internos na resposta.

O cadastro não autentica automaticamente o usuário.

## Estratégia de testes

Testes unitários verificam as regras do caso de uso,
normalização, rejeição de dados inválidos e identidade do domínio.

Testes de integração utilizam servidor HTTP em porta aleatória
e PostgreSQL temporário com Testcontainers.

Verificam cadastro, persistência, hash BCrypt, duplicidade
de e-mail, entradas inválidas e JSON malformado.

O banco de testes é separado do PostgreSQL do Docker Compose.

## Validação

- Cadastro manual pelo Compose: HTTP 201.
- Resposta sem senha ou hash.
- Testes de integração: 6 executados, sem falhas, erros ou ignorados.
- Suíte completa com ./mvnw verify: 28 testes, sem falhas,
  erros ou ignorados; BUILD SUCCESS.
- CI do PR de cadastro: concluída com sucesso, conforme resultado
  informado pelo desenvolvedor.

## Limitações e próximas etapas

- Autenticação JWT ainda não implementada.
- Violações internas de validação retornam mensagem geral;
  erros do Bean Validation incluem identificação dos campos.
- A corrida de cadastros simultâneos é protegida pela restrição
  única, mas não possui teste concorrente específico nesta etapa.