# Critérios de aceitação

## Estrutura e stack
- [ ] Projeto Maven `tesouro-monitor`, compilável com Java 25 e Spring Boot 3.x.
- [ ] Dependências: Spring Web, WebFlux, Spring Data JPA, H2 e driver PostgreSQL; Scheduling habilitado.
- [ ] Pacotes `domain`, `dto`, `repository`, `service`, `scheduler` e `config`.
- [ ] `AGENTS.md` e `docs/guardrails.md` orientam contribuições e proíbem Lombok.

## Persistência
- [ ] `AtivoCarteira` e `CotacaoDiaria` têm tabelas explícitas e todos os campos especificados.
- [ ] Entidades têm construtores sem argumentos e com argumentos, getters e setters manuais.
- [ ] Repositórios estendem `JpaRepository`; cotação expõe `findTopByNomeAtivoOrderByDataCotacaoDesc` retornando `Optional`.
- [ ] Perfil `dev` usa H2 em memória `jdbc:h2:mem:tesourodb`, porta 8080 e `ddl-auto: update`.
- [ ] Perfil `prod` configura PostgreSQL via propriedades externas.

## Integração e agendamento
- [ ] DTOs são records anotados com `@JsonIgnoreProperties(ignoreUnknown = true)`.
- [ ] `WebClientConfig` fornece um `WebClient` configurável; `TesouroDiretoClient` encapsula GET.
- [ ] `MonitoramentoService.avaliarAgio(AtivoCarteira, CotacaoDiaria)` calcula variação estimada usando PU Venda e `docs/regras-monitoramento.md`.
- [ ] Scheduler tem `@EnableScheduling` na aplicação e executa `sincronizarCotacoes` em `0 15 10 * * MON-FRI`.
- [ ] Scheduler recebe `TesouroDiretoClient` e `MonitoramentoService` por construtor.
- [ ] Mapeamento da resposta e persistência só são considerados completos após confirmação do endpoint e dos campos JSON oficiais.

## Qualidade
- [ ] Nenhuma dependência, importação ou anotação Lombok.
- [ ] Nenhum `RestTemplate`.
- [ ] Logs usam logger SLF4J declarado manualmente nas classes de serviço/scheduler.
- [ ] Testes automatizados cobrem DTOs e comportamento independente de rede; testes não exigem credenciais externas.

## Monitoramento e acesso
- [x] Cálculo usa quantidade, arredonda em duas casas `HALF_UP` e trata valores nulos ou não positivos.
- [x] Apenas cruzamento ascendente do limite configurado gera alerta; deságio não gera alerta.
- [x] Alertas são deduplicados por ativo/cotação, podem ser listados e marcados como lidos.
- [x] O painel apresenta totais, cotações indisponíveis, histórico filtrável por datas e alertas recentes.
- [x] O modo MVP single-user escuta apenas em `127.0.0.1` e está documentado como não apropriado para exposição pública.

## Pendência bloqueadora da integração
- [ ] Registrar endpoint oficial, amostra JSON e mapeamento aprovado para nome, taxa, valor de compra e valor de resgate.
