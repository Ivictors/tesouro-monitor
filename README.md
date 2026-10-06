# tesouro-monitor

Aplicação local para acompanhar títulos do Tesouro Direto. Backend Java 25/Spring Boot e frontend React/TypeScript.

## Pré-requisitos

- JDK 25 e Maven (ou `mvnw.cmd`, se incluído no checkout).
- Node.js 24 e npm.
- Docker Desktop somente para usar PostgreSQL local e executar os testes PostgreSQL com Testcontainers.

## Desenvolvimento com H2

No PowerShell, na raiz do repositório:

```powershell
Copy-Item src/main/resources/application-example.yml src/main/resources/application.yml
Copy-Item src/main/resources/application-dev-example.yml src/main/resources/application-dev.yml
```

Se esses arquivos locais já existirem, não os sobrescreva. O exemplo ativa o perfil `dev`, usa H2 em memória (`jdbc:h2:mem:tesourodb`), porta 8080 e `ddl-auto: update`. Os dados em memória são perdidos quando a aplicação para ou reinicia.

Inicie o backend:

```powershell
mvn spring-boot:run
```

Em outro PowerShell, inicie o frontend:

```powershell
Set-Location frontend
npm ci
npm run dev
```

Abra a URL que o Vite exibir. O proxy encaminha `/api` para `http://localhost:8080`.

## Persistência com PostgreSQL

PostgreSQL é o banco do perfil `prod`; H2 serve apenas para desenvolvimento rápido. O Spring Boot **não carrega `.env` automaticamente**. O Docker Compose lê `.env` para iniciar o banco, mas as variáveis da aplicação precisam ser definidas no ambiente do processo Java.

Crie o arquivo local sem sobrescrever um já existente e substitua os valores fictícios por credenciais próprias:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Inicie e confira PostgreSQL. A porta do banco fica limitada a `127.0.0.1:5432` e os dados persistem em volume Docker:

```powershell
docker compose --env-file .env -f compose-example.yml up -d
docker compose --env-file .env -f compose-example.yml ps
```

Copie os exemplos de configuração apenas se os arquivos ainda não existirem:

```powershell
if (-not (Test-Path src/main/resources/application.yml)) { Copy-Item src/main/resources/application-example.yml src/main/resources/application.yml }
if (-not (Test-Path src/main/resources/application-prod.yml)) { Copy-Item src/main/resources/application-prod-example.yml src/main/resources/application-prod.yml }
```

No PowerShell que iniciará a aplicação, configure as variáveis. Troque usuário e senha pelos valores escolhidos no `.env`:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/tesourodb'
$env:DATABASE_USERNAME = 'seu_usuario_local'
$env:DATABASE_PASSWORD = 'sua_senha_local'
mvn spring-boot:run
```

O perfil `prod` exige usuário e senha e aplica migrações Flyway; Hibernate valida o schema. Para parar o banco preservando dados:

```powershell
docker compose --env-file .env -f compose-example.yml stop
```

Não use `down -v` se quiser manter a carteira. Consulte [`docs/operacao-postgresql.md`](docs/operacao-postgresql.md) para backup e restauração; mantenha backups fora do Git.

## Sincronização

A aplicação busca o CSV oficial documentado em [`docs/integracao-tesouro.md`](docs/integracao-tesouro.md). O botão **Sincronizar cotações** atualiza sob demanda; o scheduler tenta sincronizar em dias úteis às 10h15 no fuso de São Paulo enquanto o backend está em execução. A API também oferece `GET /api/sincronizacao` para consultar o estado e `POST /api/sincronizacao` para iniciar uma sincronização. Uma execução sobreposta responde HTTP 409; falha na fonte responde HTTP 502 e preserva as cotações existentes.

Para fazer a chamada manual no PowerShell:

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/api/sincronizacao
```

## Testes e build

Na raiz, execute os testes do backend:

```powershell
mvn --batch-mode verify
```

O conjunto PostgreSQL usa Testcontainers e precisa do Docker ativo; sem Docker, os testes específicos são ignorados e essa execução não comprova o comportamento PostgreSQL. Na pasta `frontend/`:

```powershell
npm ci
npm test
npm run build
```

O CI executa Maven com Java 25, Testcontainers/PostgreSQL, testes frontend e build frontend. Resultados da entrega e da fumaça real da fonte estão em [`docs/criterios-de-aceitacao.md`](docs/criterios-de-aceitacao.md).

## Encerrar

Pare o backend com `Ctrl+C` no terminal correspondente. Pare o servidor de desenvolvimento Vite com `Ctrl+C`. Para parar apenas o PostgreSQL e preservar seu volume, use `docker compose --env-file .env -f compose-example.yml stop`.

## Segurança e limites

`.env`, arquivos Spring ativos (`.properties`, `.yml`, `.yaml`), dados e backups locais não devem ser versionados. Apenas arquivos `*-example` e `.env.example` são apropriados para o Git; exemplos não devem conter segredos. O `.env.example` contém valores fictícios de banco.

O MVP é single-user/local e backend e PostgreSQL ficam limitados a loopback por padrão. Não exponha a porta 8080 à rede ou à internet: autenticação e autorização ainda não foram implementadas. A aplicação não envia notificações externas; alertas ficam disponíveis no próprio painel/API. Veja [`docs/guardrails.md`](docs/guardrails.md) e [`docs/regras-monitoramento.md`](docs/regras-monitoramento.md).
