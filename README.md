# tesouro-monitor

Base de monitoramento de títulos do Tesouro Direto com Java 25 e Spring Boot 3.

## Configuracao local

1. Copie `.env.example` para `.env` e ajuste os valores locais. O arquivo `.env` é ignorado pelo Git.
2. Copie `src/main/resources/application-example.yml` para `application.yml` no mesmo diretório. As configurações `application-dev.yml` e `application-prod.yml` também são locais e ignoradas.
3. A fonte pública documentada em `docs/integracao-tesouro.md` fornece CSV diário; ajuste `TESOURO_API_BASE_URL` e `TESOURO_API_TITULOS_PATH` se a fonte mudar.
4. Execute com Java 25: `mvn spring-boot:run`.

Para o perfil `prod`, use `application-prod-example.yml` como modelo local, defina `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD`, e inicie com `SPRING_PROFILES_ACTIVE=prod`. Flyway aplica as migrações versionadas em `src/main/resources/db/migration`; Hibernate valida o schema.

Os arquivos `*-example.yml` e `.env.example` contêm somente valores fictícios. Spring Boot não carrega `.env` automaticamente; configure as variáveis no ambiente da IDE, terminal ou plataforma de execução.

## Testes

Execute `mvn test` com JDK 25 instalado. O teste PostgreSQL usa Testcontainers e requer Docker ativo; quando Docker não está disponível, esse teste é marcado como ignorado.

## Frontend

Em outro terminal, execute `cd frontend`, `npm ci` e `npm run dev`. O Vite encaminha `/api` para o backend em `http://localhost:8080`. Para verificar o pacote e os testes de interface, execute `npm run build` e `npm test` dentro de `frontend/`.

## Seguranca

O `.gitignore` exclui `.env`, configurações locais `.properties`/`.yml`/`.yaml`, artefatos de build, chaves privadas e arquivos de keystore. Somente arquivos com `-example` no nome são permitidos para configuração de exemplo.

O MVP é single-user e local: o backend escuta em `127.0.0.1` por padrão. Não exponha a porta 8080 à rede ou à internet. Acesso remoto exige autenticação e autorização, ainda não implementadas.
