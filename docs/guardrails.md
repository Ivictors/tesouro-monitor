# Guardrails

## Regras obrigatorias
- Java 25 e Spring Boot 3.x com Maven.
- Sem Lombok, `@Data`, `@Getter`, `@Setter`, `@NoArgsConstructor`, `@Builder`, `@Slf4j` ou equivalentes geradores.
- DTOs de integração usam `record` e `@JsonIgnoreProperties(ignoreUnknown = true)`; campos externos usam `@JsonProperty` nos parâmetros quando o contrato estiver confirmado.
- Entidades JPA usam `@Table` explícito, construtor sem argumentos, construtor com argumentos e getters/setters escritos manualmente.
- Cliente HTTP usa `WebClient`; não introduzir `RestTemplate`.
- Persistência local H2 no perfil `dev` e PostgreSQL no perfil `prod`.
- Dependências devem ficar limitadas às tecnologias listadas no pedido. Não adicionar Telegram até existir requisito para essa integração.
- Regras de negócio complexas permanecem marcadas com `// TODO: [Issue #X]` até receberem issue identificável.

## Contratos pendentes
- O SDD/API não foi encontrado no repositório. O contrato JSON, endpoint público, nomes exatos dos campos e semântica dos valores precisam ser confirmados antes de fechar o mapeamento da integração.
- O DTO inicial usa nomes convencionais provisórios documentados nos critérios de aceitação. Não tratar esses nomes como contrato validado.
- A associação entre título externo, ativo da carteira e os dois preços (`precoCompraAtual` e `precoResgateAtual`) não foi especificada.
