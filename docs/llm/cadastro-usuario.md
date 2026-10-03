# Registro de apoio de LLM — cadastro de usuário

## Objetivo

Apoiar a implementação do cadastro de usuários seguindo
Clean Architecture, princípios SOLID e conceitos de DDD.

## Apoio recebido

- Proposta da entidade de domínio Usuario.
- Proposta dos contratos UsuarioRepository e PasswordHasher.
- Proposta do caso de uso CadastrarUsuarioUseCase.
- Explicação das diferenças entre record e entidade de domínio.
- Proposta do modelo JPA, repositório e adaptador de persistência.
- Separação da conversão de objetos em UsuarioMapper.
- Implementação proposta de hash BCrypt e composição das dependências.
- Proposta do endpoint, DTOs e tratamento de erros HTTP.
- Elaboração de testes unitários e de integração.
- Apoio na documentação técnica da funcionalidade.

## Decisões do desenvolvedor

- Organizar o backend por camadas na raiz do pacote.
- Agrupar arquivos por responsabilidade dentro das camadas.
- Utilizar uma classe explícita para a entidade de domínio Usuario.
- Exigir nome com 3 a 100 caracteres após remover espaços externos.
- Manter domínio e aplicação independentes de Spring e JPA.
- Utilizar a pasta model para os modelos de persistência.
- Adotar os nomes UsuarioModel e UsuarioRepositorioJPA.
- Separar o mapeamento em uma pasta e classe específicas.

## Ajustes realizados na proposta

- Substituição do record Usuario por uma classe com campos privados.
- Inclusão de criação e reconstituição controladas.
- Definição de igualdade pelo identificador UUID.
- Ajuste dos caminhos, packages e imports ao padrão escolhido.
- Preservação de record para objetos de transferência de dados.
- Extração da conversão domínio/persistência para UsuarioMapper.

## Política de senha implementada

- Mínimo de 8 caracteres e máximo de 72 bytes em UTF-8.
- A senha não é normalizada nem tem espaços removidos.
- Armazenamento com hash BCrypt, configurado com custo 12.
- Senha e hash não são retornados pela API.

## Revisão e validação pelo desenvolvedor

O desenvolvedor aplicou as propostas, definiu ajustes de organização
e executou as verificações no Ubuntu/WSL.

Resultados informados:

- Cadastro manual pelo Docker Compose: HTTP 201.
- Resposta sem senha ou hash.
- Testes de integração: 6 executados, sem falhas, erros ou ignorados.
- Suíte completa com ./mvnw verify: 28 testes, sem falhas,
  erros ou ignorados; BUILD SUCCESS.
- CI do PR de cadastro: concluída com sucesso, conforme resultado
  informado pelo desenvolvedor.

Os testes de integração verificam persistência, correspondência
do hash BCrypt com a senha, rejeição de e-mail duplicado,
entradas inválidas e JSON malformado.

## Limites desta etapa

Login, emissão de JWT, renovação de acesso e logout
não fazem parte desta entrega de cadastro.