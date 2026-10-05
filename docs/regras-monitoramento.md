# Regras de valorização e alertas

## Decisões do MVP

- O preço atual usado como referência é o `PU Venda Manha` publicado pelo Tesouro Direto, mapeado para `CotacaoDiaria.precoResgateAtual`.
- A posição é calculada multiplicando o preço unitário pela quantidade cadastrada.
- Valor investido = `precoCompra × quantidade`.
- Valor atual estimado = `PU Venda mais recente × quantidade`.
- Variação em reais = valor atual estimado menos valor investido.
- Variação percentual = `(valor atual estimado - valor investido) / valor investido × 100`.
- Valores em reais e percentual são arredondados para duas casas decimais com `HALF_UP`.
- Uma posição sem preço ou quantidade positiva, ou sem PU Venda válido, não recebe cálculo nem alerta.
- Deságio e equilíbrio são exibidos como variação, mas nunca geram alerta.
- O limite padrão é 5%, configurável por `TESOURO_LIMITE_AGIO_PERCENTUAL`.
- Um alerta é criado quando a variação passa de abaixo de 5% para igual ou acima de 5%. Na primeira cotação disponível, considera-se a variação anterior como 0%.
- Enquanto a posição permanecer em 5% ou mais, não há alertas repetidos. Se cair abaixo do limite e voltar a cruzá-lo, um novo alerta pode ser criado.
- Uma cotação e posição geram no máximo um alerta, mesmo que a sincronização seja repetida.
- O MVP não calcula impostos, taxas ou lucro líquido. A variação é estimada e bruta.
- Alertas ficam disponíveis na aplicação. Telegram e outros canais externos ficam fora do MVP.

## Exemplos

1. Compra de 2 unidades por R$ 100,00 cada e PU Venda de R$ 102,00: investido R$ 200,00, valor atual R$ 204,00, variação R$ 4,00 ou 2,00%; sem alerta.
2. Compra de 2 unidades por R$ 100,00 cada e PU Venda de R$ 105,00: investido R$ 200,00, valor atual R$ 210,00, variação R$ 10,00 ou 5,00%; alerta no cruzamento.
3. Depois do alerta, PU Venda em R$ 106,00: variação 6,00%; sem repetição.
4. Se depois cair para R$ 104,00 (4,00%) e voltar a R$ 105,00, um novo alerta é criado no novo cruzamento.
5. Compra de 1 unidade por R$ 100,00 e PU Venda de R$ 98,00: variação -2,00%; sem alerta de deságio.

## Uso e segurança

O produto é single-user e local no MVP. O backend deve escutar apenas em `127.0.0.1` por padrão. Não exponha a porta 8080 à rede ou à internet. Autenticação multiusuário, cadastro de usuários e hospedagem pública não fazem parte desta decisão.
