# Infraestrutura inicial

## Decisões

Docker e CI foram incluídos na estrutura inicial para verificar
a execução e o build durante o desenvolvimento.

O Dockerfile utiliza dois estágios: compilação com JDK 21 e execução
com JRE 21. A aplicação executa com usuário sem privilégios administrativos.

O Compose inicia backend e PostgreSQL. O banco possui volume persistente
e verificação de disponibilidade. A aplicação ainda não utiliza o banco.

O Actuator expõe o endpoint de saúde, sem detalhes internos.

O workflow de CI está configurado para verificar o projeto com Maven
Wrapper e construir a imagem Docker. Não realiza deploy nem publicação
de imagens.

## Limitações atuais

- Persistência, migrations e autenticação ainda não implementadas.
- A CI ainda não verifica a execução integrada dos containers.
- Imagens Docker usam tags de versão; digests ainda não foram fixados.
- O ambiente local apresentou aviso de ausência do plugin Buildx.
  A imagem foi construída com sucesso pelo builder clássico.

## Validação

- Maven verify após configuração: sucesso informado pelo desenvolvedor.
- Build da imagem Docker: concluído com sucesso.
- PostgreSQL no Compose: healthy.
- Backend no Compose: em execução.
- Consulta ao endpoint /actuator/health: status UP.
- Execução no GitHub Actions: pendente.