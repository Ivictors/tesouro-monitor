# Decisão do frontend do MVP

## Escolha

React, TypeScript e Vite em `frontend/`, com execução/build independentes do Maven. O Vite encaminha `/api` para o backend local em `http://localhost:8080`; isso evita configurar CORS durante o desenvolvimento. Em produção, os arquivos estáticos podem ser hospedados atrás do mesmo domínio/proxy da API.

## Alternativas consideradas

- HTML/JavaScript servido pelo Spring: menos ferramentas e uma implantação única, mas componentes, testes de interação e crescimento para painéis ficam menos organizados.
- React/TypeScript/Vite: adiciona Node e uma etapa de build, mas dá tipagem nos contratos, testes rápidos de interface e fronteira clara com a API Java.

O repositório escolhe React/TypeScript/Vite por causa dos fluxos de formulário, carteira, histórico e painel previstos. O primeiro incremento implementa gestão da carteira. Histórico, valorização e alertas aguardam os contratos das issues #5, #7 e #10.

## Experiência e acessibilidade

1. Página inicial apresenta os ativos cadastrados e ação para incluir um ativo.
2. Formulário contém título e vencimento no formato canônico `Tipo do Título - AAAA-MM-DD`, data da compra, taxa contratada, preço de compra e quantidade.
3. Ações editar e remover ficam junto a cada linha; remover solicita confirmação.
4. Estados de carregamento, vazio, erro, validação e sucesso são comunicados com texto; formulário e tabela funcionam em tela estreita e larga.
5. Variação e alertas não são calculados pela interface; ela deverá exibir resultados fornecidos pela API depois da decisão de produto da issue #5.
