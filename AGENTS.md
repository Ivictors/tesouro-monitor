# Instrucoes do repositorio

## Escopo e stack
- Projeto `tesouro-monitor`: Java 25, Spring Boot 3.x, Maven.
- Use Spring Web, WebFlux apenas para `WebClient`, Spring Data JPA, H2 no perfil `dev`, PostgreSQL no perfil `prod` e Spring Scheduling.
- Lombok e anotacoes geradoras de codigo sao proibidos. Entidades devem declarar construtores e acessores manualmente; DTOs de integracao devem ser records.

## Arquitetura e qualidade
- Mantenha as responsabilidades nos pacotes `domain`, `dto`, `repository`, `service`, `scheduler` e `config`.
- Prefira construtor explícito para injeção, dependências pequenas e interfaces nos limites que possam receber novas integrações.
- Use logs SLF4J declarados manualmente. Nunca registre credenciais ou dados pessoais.
- Pontos de regras complexas devem conter `// TODO: [Issue #X]` até que a issue correspondente seja definida.
- Preserve o mapeamento JSON confirmado pela API. Não adivinhe nomes de campos ou significado de valores sem documentar a pendência.

## Verificação
- Testes unitários devem ser independentes de serviços externos e credenciais.
- Para mudança de código, execute os testes Maven relevantes e registre o resultado. Não declare validação sem executá-la.
- Mantenha critérios de aceitação e decisões de integração atualizados em `docs/`.
