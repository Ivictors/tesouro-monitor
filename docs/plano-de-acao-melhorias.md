# Plano de ação para melhorias e entrega

Este plano organiza a próxima entrega em etapas verificáveis. O escopo confirmado nesta rodada é padronizar PostgreSQL local/testes, validar o fluxo completo e deixar a integração para revisão manual. Nenhum pull request será mergeado pelo agente.

## Fluxo de trabalho

1. Partir de uma `main` atualizada e criar uma branch temporária exclusiva para a entrega.
2. Trabalhar em uma issue por vez; commits pequenos em português no formato Conventional Commits.
3. Executar os testes correspondentes e registrar os resultados antes de concluir a issue.
4. Enviar a branch ao GitHub, abrir PR para `main` e aguardar revisão/merge manual do usuário.
5. Não fazer push direto para `main`, não habilitar auto-merge e não fechar a issue antes da integração aprovada.

## Etapa atual — issue #19: PostgreSQL local e testes

### Implementação

- [x] Usar a imagem local `postgres:18.6` no Compose e no teste Testcontainers.
- [x] Restringir a porta publicada a `127.0.0.1:5432`.
- [x] Permitir escolher outra porta host sem remover o bind de loopback quando 5432 estiver ocupada.
- [x] Usar volume nomeado e caminho de dados compatível com PostgreSQL 18.
- [x] Documentar inicialização, prontidão, parada, persistência e cuidados ao migrar de PostgreSQL 16.

### Validação executada

- [x] `docker compose --env-file .env -f compose-example.yml config --quiet` validou a configuração.
- [x] PostgreSQL 18.6 iniciou saudável via Compose em `127.0.0.1:5433`; a porta 5432 já estava ocupada e permaneceu intacta.
- [x] `mvn --batch-mode "-Djava.version=21" verify`: 42 testes, 0 falhas, 0 erros e 2 skips. Os dois skips são `PostgresMigrationTest`: Testcontainers não detectou o endpoint Docker deste ambiente local.
- [x] `npm test`: 9 testes passaram; `npm run build` concluiu com sucesso.
- [x] Backend no perfil `prod` conectou ao Compose, aplicou as migrações Flyway e respondeu às operações da API. Reinicialização preservou os dados; backup e restauração em banco separado foram verificados.
- [x] CI no PR #20 passou com Java 25, PostgreSQL/Testcontainers, testes frontend e build frontend.

## Critérios para concluir a etapa

- Compose conecta à imagem `postgres:18.6` já disponível, mantém os dados no volume nomeado e não publica a porta fora do loopback.
- A suíte PostgreSQL exerce o mesmo major/patch configurado para desenvolvimento local.
- Maven passou localmente usando o override Java 21, pois esta máquina não tem Java 25 instalado. Os testes de integração Testcontainers foram pulados por indisponibilidade de detecção do Docker; a integração foi verificada manualmente contra o PostgreSQL Compose. O CI deve confirmar Java 25 e Testcontainers.
- A aplicação inicia com `prod`, aplica as migrações e atende a API usando PostgreSQL Compose.
- PR #20 para `main` está aberto, com CI verde, e aguarda o merge manual do usuário; não habilitar auto-merge.

## Próximas decisões

Após esta estabilização, escolher a próxima melhoria de produto antes de abrir a implementação. O backlog de issues anterior está encerrado; não assumir como requisito autenticação pública, novas telas ou descoberta automática do CSV sem decisão explícita. A aplicação permanece pessoal e local.
