# Frontend

## Stack e execução

O frontend usa React, TypeScript e Vite em `frontend/`, com execução e build independentes do Maven. Em desenvolvimento, Vite encaminha `/api` para o backend local em `http://localhost:8080`.

## MVP

- Cadastro, edição e remoção de ativos da carteira.
- Painel com valores investidos, atuais e variação estimada fornecidos pelo backend.
- Lista de posições com PU Venda e data da cotação usada.
- Histórico paginado e filtrável por ativo e intervalo de datas.
- Alertas recentes de valorização, com ação para marcar como lido.
- Estados de carregamento, vazio, erro, validação e cotação indisponível comunicados em texto.
- Tabelas responsivas com cabeçalhos para identificação das colunas.

## Regras

- A UI não calcula valores financeiros; usa os resultados da API.
- A semântica financeira está em `docs/regras-monitoramento.md`.
- O frontend é single-user/local. Não publique a aplicação antes de implementar autenticação e autorização.

## Verificação

Na pasta `frontend/`, execute `npm test` e `npm run build`.
