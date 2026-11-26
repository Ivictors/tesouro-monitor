# Operação PostgreSQL local

O perfil `prod` usa PostgreSQL; o perfil `dev` usa H2 em memória. O arquivo `compose-example.yml` inicia PostgreSQL 18.6 apenas em `127.0.0.1:5432` por padrão e persiste os dados no volume `tesouro-monitor-postgres18-data`. Se a porta 5432 já estiver ocupada, altere `POSTGRES_HOST_PORT` no `.env` e use a mesma porta em `DATABASE_URL`. Na imagem oficial do PostgreSQL 18, o volume deve ser montado em `/var/lib/postgresql`, conforme a [documentação da imagem oficial](https://hub.docker.com/_/postgres). Antes de iniciar, crie `.env` a partir de `.env.example` e troque os valores fictícios de usuário e senha.

```powershell
docker compose --env-file .env -f compose-example.yml up -d
docker compose --env-file .env -f compose-example.yml ps
```

O serviço deve aparecer como `healthy` antes de iniciar a aplicação. O volume anterior da configuração PostgreSQL 16, se existir, não é reaproveitado nem apagado por esta configuração. Uma atualização de major version exige migração dos dados; consulte o procedimento de backup abaixo antes de fazer qualquer migração.

## Backup

Com o serviço PostgreSQL ativo, execute no PowerShell. O comando cria um backup lógico em formato customizado, copiando o arquivo para `backups/`, diretório local ignorado pelo Git.

```powershell
New-Item -ItemType Directory -Force backups | Out-Null
$dbUser = Read-Host "Usuário PostgreSQL configurado no .env"
$dbName = Read-Host "Banco PostgreSQL configurado no .env"
$compose = @('--env-file', '.env', '-f', 'compose-example.yml')
$containerId = (docker compose @compose ps -q postgres).Trim()
docker compose @compose exec -T postgres pg_dump -U $dbUser -d $dbName -Fc -f /tmp/tesouro-monitor.dump
docker cp "$($containerId):/tmp/tesouro-monitor.dump" ".\backups\tesouro-monitor.dump"
```

Guarde uma cópia em local seguro. O arquivo contém dados financeiros da carteira e não deve ser enviado ao GitHub.

## Verificar uma restauração

Restaure primeiro em um banco separado, sem sobrescrever o banco em uso:

```powershell
$restoreDb = "tesouro_restore"
docker compose @compose exec -T postgres createdb -U $dbUser $restoreDb
docker cp ".\backups\tesouro-monitor.dump" "$($containerId):/tmp/tesouro-monitor.dump"
docker compose @compose exec -T postgres pg_restore -U $dbUser -d $restoreDb --no-owner --no-privileges /tmp/tesouro-monitor.dump
docker compose @compose exec -T postgres psql -U $dbUser -d $restoreDb -c "select count(*) from ativos_carteira"
```

Compare a quantidade restaurada com a esperada. Só depois de confirmar a restauração, planeje qualquer recuperação do banco principal. O teste PostgreSQL em Testcontainers também exercita backup lógico e restauração em um banco descartável.

## Parar sem apagar dados

```powershell
docker compose --env-file .env -f compose-example.yml stop
```

`docker compose down -v` remove o volume e apaga os dados; use apenas quando quiser descartar esse banco local.
