# Guardrails

## Regras obrigatorias
- Java 25 e Spring Boot 3.x com Maven.
- Sem Lombok, `@Data`, `@Getter`, `@Setter`, `@NoArgsConstructor`, `@Builder`, `@Slf4j` ou equivalentes geradores.
- DTOs de integração usam `record` e `@JsonIgnoreProperties(ignoreUnknown = true)`; campos externos usam `@JsonProperty` nos parâmetros quando o contrato estiver confirmado.
- Entidades JPA usam `@Table` explícito, construtor sem argumentos, construtor com argumentos e getters/setters escritos manualmente.
- Cliente HTTP usa `WebClient`; não introduzir `RestTemplate`.
- Persistência local H2 no perfil `dev` e PostgreSQL no perfil `prod`.
- Dependências devem ficar limitadas às tecnologias listadas no pedido. Não adicionar Telegram até existir requisito para essa integração.
- Regras de negócio sem decisão aprovada ficam marcadas com `// TODO: [Issue #X]`; após aprovação, documente a regra e remova o TODO ao implementá-la com testes.

## Integração documentada
- A fonte, os campos CSV e a semântica de PU Compra/PU Venda estão em `docs/integracao-tesouro.md`.
- O cliente mapeia PU Venda para `CotacaoDiaria.precoResgateAtual`; a regra de valorização aprovada está em `docs/regras-monitoramento.md`.

## Decisão de produto atual
- A regra aprovada está em `docs/regras-monitoramento.md`: PU Venda como valor de resgate, limite de 5% e alertas apenas no cruzamento ascendente.
- O modo inicial é single-user/local. Configure bind para `127.0.0.1`; nunca exponha diretamente a aplicação sem implementar autenticação/autorização.
