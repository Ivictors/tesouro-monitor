# Plano: carteira e monitoramento de ágio

## Objetivo

Permitir que a pessoa registre cada compra de Tesouro Direto, associe-a sem erro às cotações oficiais do mesmo título e acompanhe a valorização da posição. Quando a valorização bruta cruzar o limite configurado, a aplicação deve apresentar um alerta local. Deságio deve continuar sendo apenas informação, sem alerta.

O produto continua pessoal e local. Este plano não inclui recomendação de compra/venda, cálculo líquido de impostos/taxas, integração com corretora, Telegram, publicação pública ou múltiplos usuários.

## Estado atual verificado na `main`

O fluxo principal já existe; não deve ser reimplementado:

- `TesouroDiretoClient` baixa o CSV oficial do Tesouro Transparente e o parser escolhe a data-base mais recente.
- A sincronização persiste cotações, avalia posições e tem execução manual e agendada.
- O nome canônico usado para relacionar ativo e cotação é `Tipo Titulo - AAAA-MM-DD` (data de vencimento). Hoje a interface pede que a pessoa digite esse nome manualmente.
- O backend calcula valor investido, valor atual estimado, variação em reais e percentual bruto, usando PU Venda vezes quantidade.
- O backend gera alerta local quando cruza o limite de 5% por padrão. O limite pode ser configurado; deságio não gera aviso; alertas repetidos são evitados até que a posição volte abaixo do limite.
- A interface mostra posições, cotações, variação, data da cotação e alertas recentes.

Referências de regra: [`regras-monitoramento.md`](regras-monitoramento.md), [`integracao-tesouro.md`](integracao-tesouro.md) e [`frontend.md`](frontend.md).

## Lacunas para fechar a experiência

1. **Vinculação manual frágil:** o formulário aceita texto livre. Diferença de grafia ou vencimento impede encontrar a cotação correspondente. A recomendação é escolher o título a partir da lista oficial já sincronizada e guardar exatamente a chave canônica que o backend usa.
2. **Limite divergente na interface:** o backend permite configurar o limite, mas a seção de alertas apresenta `≥ 5%` fixo. A interface deve obter o limite efetivo da API ou usar um texto que não contradiga a configuração.
3. **Estado de monitoramento pouco explícito:** mostrar claramente a data-base da cotação, o resultado em relação ao limite e quando não existe cotação correspondente. Não inventar um SLA nem classificar uma cotação como vencida sem decisão de idade máxima.
4. **Fluxo de ponta a ponta:** validar o caminho sincronizar → selecionar título → cadastrar compra → consultar posição → cruzar limite → ler alerta, incluindo falha da fonte e ausência de correspondência.

## Decisões de implementação definidas

Conforme a issue #21, o cadastro seleciona o título do catálogo oficial sincronizado; nome livre não é o fluxo de aceite. O catálogo vem das cotações mais recentes e mantém a chave canônica `Tipo Titulo + Data Vencimento`. Cada compra fica em uma linha/lote independente, permitindo datas e preços diferentes para o mesmo título. O backend continua responsável pelos cálculos e pelo alerta; o frontend apresenta os resultados.

## Critérios de aceitação

- **AC-01 — Seleção oficial:** dado que há cotações sincronizadas, quando a pessoa cadastrar uma compra, a interface oferece os títulos encontrados nessas cotações e grava a chave canônica correspondente; não exige digitação livre do nome.
- **AC-02 — Sem catálogo:** se ainda não há cotações ou a sincronização falha, a interface explica como sincronizar e não sugere títulos fictícios.
- **AC-03 — Compra por lote:** cada registro mantém data de compra, taxa contratada, preço unitário pago e quantidade. Compras diferentes do mesmo título podem ser registradas e avaliadas separadamente.
- **AC-04 — Posição:** após uma sincronização bem-sucedida, cada lote associado exibe PU Venda, data-base utilizada, valor investido, valor atual estimado e variação em reais e percentual, calculados pelo backend conforme `regras-monitoramento.md`.
- **AC-05 — Ágio:** ao cruzar de abaixo para igual/acima do limite efetivamente configurado, o sistema cria um alerta local com percentual, limite e data-base. Repetir a mesma sincronização não duplica o alerta.
- **AC-06 — Sem aviso de deságio:** variação negativa não cria alerta. Se a posição voltar abaixo do limite e cruzá-lo novamente, pode criar um novo alerta.
- **AC-07 — Limite coerente:** a interface informa o limite realmente usado pelo backend, inclusive quando configurado por ambiente; não exibe um valor fixo incorreto.
- **AC-08 — Cotação indisponível:** a carteira permanece intacta e identifica posições sem cotação correspondente; falha da fonte não apaga cotações existentes nem apresenta um preço antigo como se fosse recém-consultado.
- **AC-09 — Privacidade:** a carteira e os alertas permanecem locais; nenhuma credencial ou informação da carteira é enviada à fonte pública.
- **AC-10 — Qualidade:** testes unitários cobrem cálculo e cruzamentos; testes de integração cobrem persistência e sincronização; testes frontend cobrem seleção, estados vazios/erro e apresentação do limite; CI executa backend e frontend.

## Etapas de implementação propostas

### Etapa 0 — Preparação e baseline

- Preservar as regras já aprovadas em `docs/regras-monitoramento.md`.
- Confirmar nomes/campos do CSV oficial e a chave canônica com a fixture e o parser existentes.
- Issues de execução abertas e ligadas aos ACs acima: [#21 catálogo oficial](https://github.com/Ivictors/tesouro-monitor/issues/21), [#22 limite e estado da cotação](https://github.com/Ivictors/tesouro-monitor/issues/22), [#23 testes do fluxo](https://github.com/Ivictors/tesouro-monitor/issues/23).
- **Aceite:** contrato do catálogo e formato da chave documentados; nenhuma regra financeira nova introduzida.

### Etapa 1 — Catálogo oficial para cadastro

Issue: [#21](https://github.com/Ivictors/tesouro-monitor/issues/21).

- Expor endpoint de leitura dos títulos distintos disponíveis na data-base mais recente, usando somente cotações importadas.
- Fazer o formulário carregar e selecionar título pelo nome oficial e vencimento; manter campos de compra por lote.
- Tratar catálogo vazio, erro de rede e sincronização ainda não executada.
- **Testes:** serviço/repositório com nomes repetidos em datas diferentes; endpoint; componentes React para seleção, loading, vazio e erro.
- **Aceite:** AC-01, AC-02 e AC-03.

### Etapa 2 — Expor limite e tornar o estado legível

Issue: [#22](https://github.com/Ivictors/tesouro-monitor/issues/22).

- Expor o limite efetivo usado na configuração do backend por contrato de leitura apropriado.
- Remover o texto `≥ 5%` fixo e apresentar o limite recebido da API.
- Diferenciar posição abaixo do limite, posição em/ acima do limite, sem cotação correspondente e falha/ausência de sincronização; sempre exibir a data-base.
- **Testes:** valor padrão, valor configurado, contrato API e renderização sem cálculo financeiro no frontend.
- **Aceite:** AC-04, AC-07 e AC-08.

### Etapa 3 — Completar cenários de sincronização e alertas

Issue: [#23](https://github.com/Ivictors/tesouro-monitor/issues/23).
Executar após as mudanças de contrato e interface das issues #21 e #22, para validar o fluxo com as decisões finais já aplicadas.

- Confirmar que uma sincronização atualiza cotações e avalia todos os lotes associados à chave oficial.
- Confirmar deduplicação, novo cruzamento após retorno abaixo do limite e comportamento em falha parcial.
- Preservar cotações já persistidas se a fonte falhar ou retornar payload inválido.
- **Testes:** unitários para limiar e cruzamento; integração com PostgreSQL/Testcontainers; fonte HTTP simulada; regressão sem chamada externa.
- **Aceite:** AC-05, AC-06, AC-08 e AC-09.

### Etapa 4 — Validação ponta a ponta e entrega

Complementa a issue [#23](https://github.com/Ivictors/tesouro-monitor/issues/23).

- Automatizar o fluxo principal desde sincronização com fonte simulada até posição e alerta apresentados.
- Executar `mvn verify`, `npm test`, `npm run build` e a suíte PostgreSQL no CI.
- Atualizar critérios de aceitação, decisões de integração, README e instruções de uso.
- **Aceite:** AC-10; CI verde; PR aberto para revisão humana, sem merge automático.

## Questões não bloqueantes

- **Q-02 — Idade máxima da cotação:** não existe prazo de validade aprovado. Exibir a data-base e a última sincronização, sem inventar um prazo para classificar uma cotação como desatualizada. Definir limite de idade pode ficar para depois; não bloqueia as issues #21–#23.
- **Q-03 — Canal do alerta:** manter alertas dentro da aplicação, conforme escopo atual. Telegram/e-mail ficam fora deste plano; não bloqueia.

## Branch e fluxo de entrega

- Branch temporária: `feature/plano-carteira-agio`, criada diretamente de `origin/main` sem atualizar a branch local `main`.
- Manter o PR #20 independente e inalterado; esta funcionalidade não deve depender de ele ser mergeado.
- Quando a implementação for autorizada: uma issue por vez, commit Conventional Commit em português por tarefa, testes relevantes e PR para revisão humana. Não fazer merge para `main`.
