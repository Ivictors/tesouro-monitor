# Fonte de dados do Tesouro Direto

## Fonte oficial verificada em 2026-10-04

- Página do conjunto: https://www.tesourotransparente.gov.br/ckan/dataset/taxas-dos-titulos-ofertados-pelo-tesouro-direto
- API CKAN para metadados e URL do recurso: https://www.tesourotransparente.gov.br/ckan/api/3/action/package_show?id=taxas-dos-titulos-ofertados-pelo-tesouro-direto
- CSV publicado no metadado do conjunto: https://www.tesourotransparente.gov.br/ckan/dataset/df56aa42-484a-4a59-8184-7676580c81e3/resource/796d2059-14e9-44e3-80c9-2d9e30b405c1/download/precotaxatesourodireto.csv
- PDF oficial de metadados: https://www.tesourotransparente.gov.br/ckan/dataset/df56aa42-484a-4a59-8184-7676580c81e3/resource/1a8eb2e3-4902-4a38-a1eb-6410f23d90de/download/taxa.pdf

O conjunto publica diariamente taxas e preços. O CSV usa `;` como delimitador, vírgula decimal e datas `dd/MM/yyyy`. Ele contém histórico desde dezembro de 2004; a resposta CSV completa tinha cerca de 14 MB na verificação. A aplicação deve selecionar a maior `Data Base` e usar as linhas desse dia para a sincronização corrente.

## Campos do CSV

| Coluna | Significado documentado | Uso no domínio |
| --- | --- | --- |
| `Tipo Titulo` | Denominação do título | `nomeAtivo` / nome da posição |
| `Data Vencimento` | Vencimento do título | Identificador complementar; deve ser preservado no DTO |
| `Data Base` | Data de referência da cotação | `dataCotacao` |
| `Taxa Compra Manha` | Taxa de compra | Preservada no DTO |
| `Taxa Venda Manha` | Taxa de venda/recompra | `taxaAtual` no modelo inicial |
| `PU Compra Manha` | PU de compra, liquidação D+1 | `precoCompraAtual` |
| `PU Venda Manha` | PU de venda ao Tesouro, liquidação D+1 | `precoResgateAtual` |
| `PU Base Manha` | PU base para marcação a mercado D0 | Preservada no DTO; sua utilização em valor de posição depende da regra do produto |

A fixture pequena em `src/test/resources/tesouro/tesouro-direto.csv` reproduz as colunas e alguns registros publicados. É somente dado público de teste, não configuração nem dado pessoal.

## Verificação manual da fonte

Em 2026-10-05, o recurso CSV foi consultado diretamente e pela sincronização manual local da aplicação. O servidor respondeu HTTP 200 com `text/csv`; o arquivo tinha 14.561.842 bytes e o cabeçalho esperado. A aplicação interpretou e persistiu 58 cotações, sem posições correspondentes porque a carteira de teste estava vazia. Essa verificação confirma o comportamento observado nessa data; não implica SLA nem substitui os testes automatizados com HTTP simulado.

## Limites e decisões pendentes

O endpoint JSON antigo encontrado em exemplos de terceiros respondeu HTTP 410 em 2026-10-04; ele não é tratado como API suportada. A fonte oficial verificada é CSV, não JSON. A fixture documenta o contrato de dados disponível, sem afirmar que existe SLA ou API REST de cotações.

O modelo inicial não tem campo para PU Base. A issue #5 deve definir se os cálculos de valor atual/ágio usam PU Venda (recompra D+1) ou PU Base (marcação D0), e se o modelo deve armazenar ambos. O limite de alerta também depende de decisão de produto.
