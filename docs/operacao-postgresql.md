# Operação PostgreSQL local

O perfil `prod` usa PostgreSQL; o perfil `dev` usa H2 em memória. O arquivo `compose-example.yml` inicia PostgreSQL 16 apenas em `127.0.0.1:5432` e persiste os dados em volume nomeado. Antes de iniciar, crie `.env` a partir de `.env.example` e troque os valores fictícios de usuário e senha.

## Backup

Com o serviço PostgreSQL ativo, execute no PowerShell. O comando cria um backup lógico em formato customizado, copiando o arquivo para `backups/`, diretório local ignorado pelo Git.

```powershell
New-Item -ItemType Directory -Force backups | Out-Null
$dbUser = Read-Host "Usuário PostgreSQL configurado no .env"
$dbName = Read-Host "Banco PostgreSQL configurado no .env"
$containerId = (docker compose -f compose-example.yml ps -q postgres).Trim()
docker compose -f compose-example.yml exec -T postgres pg_dump -U $dbUser -d $dbName -Fc -f /tmp/tesouro-monitor.dump
docker cp "$($containerId):/tmp/tesouro-monitor.dump" ".\backups\tesouro-monitor.dump"
```

Guarde uma cópia em local seguro. O arquivo contém dados financeiros da carteira e não deve ser enviado ao GitHub.

## Verificar uma restauração

Restaure primeiro em um banco separado, sem sobrescrever o banco em uso:

```powershell
$restoreDb = "tesouro_restore"
docker compose -f compose-example.yml exec -T postgres createdb -U $dbUser $restoreDb
docker cp ".\backups\tesouro-monitor.dump" "$($containerId):/tmp/tesouro-monitor.dump"
docker compose -f compose-example.yml exec -T postgres pg_restore -U $dbUser -d $restoreDb --no-owner --no-privileges /tmp/tesouro-monitor.dump
docker compose -f compose-example.yml exec -T postgres psql -U $dbUser -d $restoreDb -c "select count(*) from ativos_carteira"
```

Compare a quantidade restaurada com a esperada. Só depois de confirmar a restauração, planeje qualquer recuperação do banco principal. O teste PostgreSQL em Testcontainers também exercita backup lógico e restauração em um banco descartável.

## Parar sem apagar dados

```powershell
docker compose -f compose-example.yml stop
```

`docker compose down -v` remove o volume e apaga os dados; use apenas quando quiser descartar esse banco local.
