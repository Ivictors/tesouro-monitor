# Plano de implementação — Issue #21

## Escopo

Implementar o cadastro de compras escolhendo um título que já exista nas cotações oficiais importadas. O endpoint e a interface devem usar a chave canônica que o monitoramento já espera. O preço pago, taxa contratada, data da compra e quantidade continuam sendo dados informados para cada compra.

Issue: [#21 — Selecionar títulos do catálogo oficial](https://github.com/Ivictors/tesouro-monitor/issues/21).

## Contrato técnico proposto

- **Fonte do catálogo:** tabela `cotacoes_diarias`; não consultar a fonte pública ao abrir o formulário.
- **Última data-base:** maior `dataCotacao` existente na tabela, considerada globalmente, conforme o critério da issue.
- **Título retornado:** `nomeAtivo`, que já está no formato canônico `Tipo Titulo - AAAA-MM-DD` (vencimento). Não reconstruir nem normalizar esse valor na UI.
- **Endpoint:** `GET /api/titulos`.
- **Resposta:** `200 OK` com lista ordenada, por exemplo `[{"nomeTitulo":"Tesouro Selic - 2031-03-01"}]`; sem cotações, `200 OK` com `[]`.
- **Persistência da compra:** manter o contrato existente `POST /api/ativos` e o campo `nomeTitulo`. Nenhuma migration ou alteração de entidade é necessária.

## Tarefas na ordem de execução

### T1 — Catálogo no backend

1. Criar teste do repositório para cotação vazia, data-base mais recente, nomes únicos e ordenação determinística. Incluir um título que exista apenas em uma data-base antiga e confirmar que não aparece.
2. Criar teste do serviço e do endpoint para lista vazia e lista preenchida.
3. Implementar consulta de nomes distintos da maior `dataCotacao`, mapear para um DTO de resposta explícito e expor `GET /api/titulos`.
4. Confirmar que o endpoint é somente de leitura e não consulta a API externa.

### T2 — Seleção no frontend

1. Adicionar o tipo do título disponível e a chamada `GET /api/titulos` em `frontend/src/api/`.
2. Carregar opções sem bloquear o carregamento da carteira; manter estado próprio de carregamento, vazio e erro do catálogo.
3. Trocar o campo livre por um `<select>` controlado pelo `nomeTitulo` canônico. Enviar o restante do formulário sem alteração.
4. No estado vazio, explicar que a pessoa deve sincronizar cotações antes de cadastrar. Em falha, exibir erro e permitir nova tentativa. Não mostrar opções de exemplo.
5. Preservar edição de registros existentes: se um ativo salvo não estiver no catálogo atual, manter seu valor existente selecionável durante a edição, identificado como ativo já cadastrado; esse valor não entra nas opções de novos cadastros.
6. Permitir cadastrar mais de uma compra do mesmo título; cada envio continua criando um registro/lote independente.

### T3 — Validação da issue

- Executar testes backend relevantes e `mvn verify`.
- Executar testes frontend e build (`npm test` e `npm run build`).
- Conferir manualmente: sem cotações, sincronizar, selecionar título oficial, cadastrar duas compras do mesmo título com dados de compra diferentes e confirmar que ambas aparecem e correspondem à mesma cotação.
- Atualizar os critérios da issue e registrar evidências antes de abrir o PR.

## Critérios de aceite e testes

| Critério da issue | Teste/evidência prevista |
| --- | --- |
| Endpoint lista títulos distintos da última data-base com chave/nome oficiais | Teste de repositório com datas diferentes e nomes repetidos; teste do endpoint e formato JSON |
| Formulário seleciona título oficial mantendo campos de compra | Teste React seleciona opção e verifica o corpo do POST com nome canônico, data, taxa, preço e quantidade |
| Compras do mesmo título permanecem lotes independentes | Teste de serviço/API e fluxo de UI com duas submissões; ambas persistem como ativos distintos |
| Estados sem catálogo, carregamento e erro são claros | Testes React para loading, lista vazia, erro, retry e opções presentes |
| Testes backend e frontend cobrem catálogo/seleção | `mvn verify`, `npm test` e `npm run build` aprovados no CI |

## Limites desta issue

- Não alterar cálculo de ágio, limite, alertas ou regra de deságio.
- Não alterar importação CSV, frequência de sincronização ou fonte oficial.
- Não adicionar preço de compra automático: a cotação atual é referência de mercado; os dados da compra continuam informados pela pessoa.
- Não criar migração de banco, autenticação, Telegram, novas telas ou edição de regras financeiras.
- A issue #22 e a issue #23 continuam separadas; não incluir seus critérios como condição para concluir esta issue.

## Commits

Aplicar TDD por fatia, com testes primeiro e implementação mínima depois. Usar commits Conventional Commits em português por tarefa concluída; validar todos os critérios da issue antes do PR. O PR aguarda revisão e merge manual do usuário.
