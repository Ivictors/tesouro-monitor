# tesouro-monitor

Base de monitoramento de títulos do Tesouro Direto com Java 25 e Spring Boot 3.

## Configuracao local

1. Copie `.env.example` para `.env` e ajuste os valores locais. O arquivo `.env` é ignorado pelo Git.
2. Copie `src/main/resources/application-example.yml` para `application.yml` no mesmo diretório. As configurações `application-dev.yml` e `application-prod.yml` também são locais e ignoradas.
3. Substitua os endereços de exemplo pelo endpoint e contrato JSON confirmados do Tesouro Direto.
4. Execute com Java 25: `mvn spring-boot:run`.

Os arquivos `*-example.yml` e `.env.example` contêm somente valores fictícios. Spring Boot não carrega `.env` automaticamente; configure as variáveis no ambiente da IDE, terminal ou plataforma de execução.

## Testes

Execute `mvn test` com JDK 25 instalado.

## Seguranca

O `.gitignore` exclui `.env`, configurações locais `.properties`/`.yml`/`.yaml`, artefatos de build, chaves privadas e arquivos de keystore. Somente arquivos com `-example` no nome são permitidos para configuração de exemplo.
