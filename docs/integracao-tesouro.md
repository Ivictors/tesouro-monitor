# Integração com dados do Tesouro Direto

## Fonte e contrato confirmado

Fonte oficial consultada em 2026-10-05:

- [Conjunto de taxas dos títulos ofertados](https://www.tesourotransparente.gov.br/ckan/dataset/taxas-dos-titulos-ofertados-pelo-tesouro-direto)
- [Metadados CKAN do conjunto](https://www.tesourotransparente.gov.br/ckan/api/3/action/package_show?id=taxas-dos-titulos-ofertados-pelo-tesouro-direto)
- [Recurso CSV configurado](https://www.tesourotransparente.gov.br/ckan/dataset/df56aa42-484a-4a59-8184-7676580c81e3/resource/796d2059-14e9-44e3-80c9-2d9e30b405c1/download/precotaxatesourodireto.csv)
- [Metadados do recurso](https://www.tesourotransparente.gov.br/ckan/dataset/df56aa42-484a-4a59-8184-7676580c81e3/resource/1a8eb2e3-4902-4a38-a1eb-6410f23d90de/download/taxa.pdf)

O cliente faz GET do CSV, separado por ponto e vírgula, com números decimais separados por vírgula e datas `dd/MM/yyyy`. O parser valida o cabeçalho, seleciona a maior `Data Base` e importa as linhas dessa data. O CSV inclui histórico desde 2004, portanto a aplicação baixa o recurso inteiro a cada sincronização.

## Mapeamento usado

| Coluna CSV | Uso na aplicação |
| --- | --- |
| `Tipo Titulo` | Nome do ativo/cotação |
| `Data Vencimento` | Complemento para distinguir títulos com o mesmo nome |
| `Data Base` | Data da cotação |
| `Taxa Venda Manha` | `CotacaoDiaria.taxaAtual` |
| `PU Compra Manha` | `CotacaoDiaria.precoCompraAtual` |
| `PU Venda Manha` | `CotacaoDiaria.precoResgateAtual`, valor de resgate D+1 |
| `Taxa Compra Manha` | Mantida no DTO de integração |
| `PU Base Manha` | Mantido no DTO; não usado no cálculo de posição |

A regra de valorização usa PU Venda multiplicado pela quantidade e está detalhada em [`regras-monitoramento.md`](regras-monitoramento.md). O parser e seu cabeçalho estão em `src/main/java/br/com/tesouromonitor/service/TesouroCsvParser.java`; uma amostra pública reduzida para testes está em `src/test/resources/tesouro/tesouro-direto.csv`.

## Configuração

O cliente usa por padrão `https://www.tesourotransparente.gov.br` e o caminho do CSV acima. `TESOURO_API_BASE_URL` e `TESOURO_API_TITULOS_PATH` permitem substituir esses valores. A URL completa é a concatenação da base com o caminho. Os limites de conexão, resposta e memória também são configuráveis no `application-example.yml`.

Não há descoberta dinâmica de recursos CKAN no MVP. Se o recurso for renomeado ou substituído, verifique a página e os metadados oficiais e atualize a configuração local. Isso evita depender de uma API intermediária a cada execução; automatizar descoberta pode ser reavaliado se a URL mudar com frequência.

## Verificação e limites

Em 2026-10-05, a consulta direta retornou HTTP 200, `text/csv` e 14.561.842 bytes. A sincronização manual local interpretou e persistiu 58 cotações da data-base mais recente. Não havia ativos correspondentes na carteira de teste, então nenhuma posição foi avaliada. Os testes automatizados usam servidor HTTP local simulado e fixture pequena, sem rede externa.

Esses resultados são uma observação pontual, não um SLA. O identificador e o caminho do recurso podem mudar, e a aplicação não garante disponibilidade da fonte. O endpoint JSON antigo citado por exemplos não oficiais respondeu HTTP 410 em 2026-10-04 e não é usado; a integração implementada é CSV.
