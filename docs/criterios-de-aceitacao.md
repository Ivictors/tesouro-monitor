# Critérios de aceitação e entrega

Este documento descreve o MVP pessoal implementado. Os critérios marcados foram verificados no CI Java 25 e Node 24; as limitações conhecidas estão na seção final.

## Stack e arquitetura

- [x] Aplicação Maven `tesouro-monitor` em Java 25 e Spring Boot 3.
- [x] Spring Web, WebFlux para `WebClient`, Spring Data JPA, H2, PostgreSQL, Flyway e Spring Scheduling.
- [x] Camadas de domínio, DTO, repositório, serviço, scheduler, configuração e API.
- [x] Entidades JPA com tabelas explícitas, construtores e acessores manuais; sem Lombok ou geradores equivalentes.
- [x] DTOs de integração imutáveis em `record`; logs SLF4J declarados manualmente.

## Persistência e integração

- [x] Perfil `dev` usa H2 em memória (`jdbc:h2:mem:tesourodb`), porta 8080 e Hibernate `update`.
- [x] Perfil `prod` usa PostgreSQL, credenciais obrigatórias externas e schema versionado por Flyway; Hibernate valida o schema.
- [x] Migrações e persistência de ativos, cotações e alertas verificadas com PostgreSQL/Testcontainers.
- [x] Reinicialização da aplicação preserva os registros; backup/restauração PostgreSQL foi exercitado em banco separado.
- [x] Cliente `WebClient` consulta o CSV oficial do Tesouro Transparente; contrato e mapeamento estão em [`integracao-tesouro.md`](integracao-tesouro.md).
- [x] Sincronização manual e agendada importam cotações sem apagar os dados anteriores em caso de erro.

## Regras de negócio e aplicação

- [x] Cálculo usa PU Venda, quantidade e regra aprovada em [`regras-monitoramento.md`](regras-monitoramento.md).
- [x] Apenas cruzamento ascendente do limite configurado de 5% gera alerta; deságio não gera aviso.
- [x] Alertas são deduplicados por ativo/cotação, listáveis e marcáveis como lidos.
- [x] Frontend permite gerir carteira, consultar painel, histórico, cotações e alertas; regras financeiras permanecem no backend.
- [x] API e frontend têm estados de erro/carregamento e sincronização manual.
- [x] Serviço é local e single-user, vinculado a `127.0.0.1`; não há autenticação nem suporte para acesso remoto.

## Qualidade e segurança

- [x] Testes backend cobrem unidades, APIs e fluxo CSV com servidor HTTP local simulado, sem dependência de credenciais externas.
- [x] Testes frontend e build executam no CI.
- [x] Testes PostgreSQL usam Testcontainers e são ignorados localmente quando Docker não está disponível.
- [x] `.env`, configuração ativa, backups, dependências e artefatos de build estão ignorados pelo Git; apenas exemplos de configuração são versionados.
- [x] Revisão do repositório não encontrou `.env`, configuração ativa, carteira local, backups ou artefatos de build entre os arquivos versionados.

## Evidências da entrega

- CI completo Java 25 + fluxo CSV + frontend: [execução 37401422400](https://github.com/Ivictors/tesouro-monitor/actions/runs/37401422400), 41 testes Maven e testes/build frontend aprovados.
- CI PostgreSQL, incluindo reinicializações e backup/restauração: [execução 37402210804](https://github.com/Ivictors/tesouro-monitor/actions/runs/37402210804), 42 testes Maven e testes/build frontend aprovados.
- Fumaça da fonte oficial em 2026-10-05: HTTP 200, `text/csv`, arquivo de 14.561.842 bytes, 58 cotações importadas. Não havia posições na carteira de teste. Detalhes e limites em [`integracao-tesouro.md`](integracao-tesouro.md).
- Configuração Compose conferida com `docker compose --env-file .env.example -f compose-example.yml config`.

## Limitações conhecidas

- O recurso CSV é uma URL configurável e pode mudar; a aplicação não descobre dinamicamente a URL pelo catálogo CKAN e a fonte não oferece SLA documentado.
- A sincronização inicial baixa o CSV histórico completo antes de selecionar a data-base mais recente.
- A aplicação não pode ser exposta à rede ou à internet até haver autenticação e autorização.
- Testes de integração PostgreSQL precisam de Docker ativo para executar fora do CI.
