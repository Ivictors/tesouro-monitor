# tesouro-monitor

Base de monitoramento de títulos do Tesouro Direto com Java 25 e Spring Boot 3.

## Configuracao local

1. Copie `.env.example` para `.env` e ajuste os valores locais. O arquivo `.env` é ignorado pelo Git.
2. Copie `src/main/resources/application-example.yml` para `application.yml` no mesmo diretório. As configurações `application-dev.yml` e `application-prod.yml` também são locais e ignoradas.
3. A fonte pública documentada em `docs/integracao-tesouro.md` fornece CSV diário; ajuste `TESOURO_API_BASE_URL` e `TESOURO_API_TITULOS_PATH` se a fonte mudar.
4. Execute com Java 25: `mvn spring-boot:run`.

Para o perfil `prod`, use `application-prod-example.yml` como modelo local, defina `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD`, e inicie com `SPRING_PROFILES_ACTIVE=prod`. Flyway aplica as migrações versionadas em `src/main/resources/db/migration`; Hibernate valida o schema.

### PostgreSQL de produção (local)

PostgreSQL é o banco padrão do perfil `prod`; H2 fica restrito ao perfil `dev`. Para iniciar o PostgreSQL local com Docker Compose, copie `.env.example` para `.env`, substitua os valores fictícios por valores locais e execute:

```powershell
docker compose -f compose-example.yml up -d
docker compose -f compose-example.yml ps
```

Em um PowerShell separado, configure `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL=jdbc:postgresql://localhost:5432/tesourodb`, `DATABASE_USERNAME` e `DATABASE_PASSWORD` com os mesmos valores locais do `.env`, e execute `mvn spring-boot:run`. Se ainda não existirem, copie `application-prod-example.yml` para `application-prod.yml` e `application-example.yml` para `application.yml`, sem sobrescrever arquivos locais existentes. O perfil `prod` falha ao iniciar se as credenciais não forem configuradas.

O Compose guarda os dados em um volume nomeado. Para parar o banco sem remover os dados, use `docker compose -f compose-example.yml stop`. Não use `down -v` se quiser preservar a carteira. O procedimento de backup e restauração está em [`docs/operacao-postgresql.md`](docs/operacao-postgresql.md); mantenha os arquivos de backup locais, na pasta `backups/`, que é ignorada pelo Git.

Os arquivos `*-example.yml` e `.env.example` contêm somente valores fictícios. Spring Boot não carrega `.env` automaticamente; configure as variáveis no ambiente da IDE, terminal ou plataforma de execução.

## Testes

Execute `mvn test` com JDK 25 instalado. O teste PostgreSQL usa Testcontainers e requer Docker ativo; quando Docker não está disponível, esse teste é marcado como ignorado.

## Frontend

Em outro terminal, execute `cd frontend`, `npm ci` e `npm run dev`. O Vite encaminha `/api` para o backend em `http://localhost:8080`. Para verificar o pacote e os testes de interface, execute `npm run build` e `npm test` dentro de `frontend/`.

### Sincronização de cotações

Use o botão **Sincronizar cotações** no painel para atualizar os dados imediatamente. A aplicação também tenta sincronizar automaticamente em dias úteis às 10h15 (horário de São Paulo), desde que esteja em execução. O painel informa o resultado e o horário da última tentativa nesta execução.

Para consultar o estado pela API, use `GET /api/sincronizacao`. Para iniciar uma sincronização manualmente sem o frontend, use `POST /api/sincronizacao` (por exemplo, `Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/sincronizacao`). Uma execução simultânea retorna HTTP 409; uma falha na fonte retorna HTTP 502 e não remove cotações já persistidas.

## Seguranca

O `.gitignore` exclui `.env`, configurações locais `.properties`/`.yml`/`.yaml`, artefatos de build, chaves privadas e arquivos de keystore. Somente arquivos com `-example` no nome são permitidos para configuração de exemplo.

O MVP é single-user e local: o backend escuta em `127.0.0.1` por padrão. Não exponha a porta 8080 à rede ou à internet. Acesso remoto exige autenticação e autorização, ainda não implementadas.
