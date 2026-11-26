import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ativosApi } from './ativos'

const jsonResponse = (body: unknown, status = 200) => ({
  ok: status >= 200 && status < 300,
  status,
  json: vi.fn().mockResolvedValue(body),
}) as unknown as Response

describe('ativosApi', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('usa endpoints GET/PATCH e envia Content-Type JSON', async () => {
    const responses = [
      [{ nomeTitulo: 'Tesouro Selic - 2031-03-01' }],
      { sucesso: true, mensagem: 'Sincronização concluída.' },
      [],
      { posicoes: [] },
      {
        alertas: [{ id: 9, ativoId: 3, nomeAtivo: 'Tesouro IPCA+ 2035', percentualObservado: 8.1,
          limitePercentual: 7.25, dataCotacao: '2026-11-25', regraAplicada: 'CRUZAMENTO_ASCENDENTE',
          criadoEm: '2026-11-25T12:00:00Z', lido: false, lidoEm: null }],
        pagina: 0, tamanho: 20, total: 1, limitePercentual: 7.25,
      },
      { id: 9, lido: true, limitePercentual: 7.25 },
    ]
    responses.forEach((response) => fetchMock.mockResolvedValueOnce(jsonResponse(response)))

    await ativosApi.titulosDisponiveis()
    await ativosApi.statusSincronizacao()
    await ativosApi.listar()
    await ativosApi.resumo()
    const alerts = await ativosApi.alertas()
    await ativosApi.marcarAlertaComoLido(9)

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      '/api/titulos',
      '/api/sincronizacao',
      '/api/ativos',
      '/api/ativos/resumo',
      '/api/alertas?tamanho=20',
      '/api/alertas/9/lido',
    ])
    expect(fetchMock.mock.calls.map(([, init]) => init?.method)).toEqual([
      undefined, undefined, undefined, undefined, undefined, 'PATCH',
    ])
    expect(fetchMock.mock.calls.every(([, init]) => init?.headers?.['Content-Type'] === 'application/json'))
      .toBe(true)
    expect(alerts).toMatchObject({ limitePercentual: 7.25 })
    expect(alerts.alertas[0]).toMatchObject({ limitePercentual: 7.25 })
  })

  it('envia os bodies de POST/PUT e aceita DELETE com resposta 204', async () => {
    const ativo = {
      nomeTitulo: 'Tesouro IPCA+ - 2035-05-15',
      dataCompra: '2026-02-03',
      taxaContratada: 0.07,
      precoCompra: 123.45,
      quantidade: 1.5,
    }
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ id: 3, ...ativo }, 201))
      .mockResolvedValueOnce(jsonResponse({ id: 3, ...ativo, quantidade: 2 }, 200))
      .mockResolvedValueOnce({ ok: true, status: 204, json: vi.fn() } as unknown as Response)

    await ativosApi.criar(ativo)
    await ativosApi.atualizar(3, { ...ativo, quantidade: 2 })
    const removed = await ativosApi.remover(3)

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      '/api/ativos', '/api/ativos/3', '/api/ativos/3',
    ])
    expect(fetchMock.mock.calls.map(([, init]) => init?.method)).toEqual(['POST', 'PUT', 'DELETE'])
    expect(fetchMock.mock.calls.every(([, init]) => init?.headers?.['Content-Type'] === 'application/json'))
      .toBe(true)
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual(ativo)
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({ ...ativo, quantidade: 2 })
    expect(fetchMock.mock.calls[2][1].body).toBeUndefined()
    expect(removed).toBeUndefined()
  })

  it('conserva o contrato de sincronização com monitoramento parcial', async () => {
    const status = {
      emAndamento: false,
      iniciadaEm: '2026-11-26T10:00:00Z',
      concluidaEm: '2026-11-26T10:01:00Z',
      sucesso: true,
      cotacoesPersistidas: 12,
      posicoesCorrespondentes: 4,
      mensagem: 'Cotações importadas, mas uma posição não foi avaliada.',
      avaliacoesFalhas: 1,
      monitoramentoCompleto: false,
    }
    fetchMock.mockResolvedValueOnce(jsonResponse(status))

    await expect(ativosApi.sincronizar()).resolves.toEqual(status)
    expect(fetchMock).toHaveBeenCalledWith('/api/sincronizacao', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock.mock.calls[0][1].headers).toEqual({ 'Content-Type': 'application/json' })
  })

  it.each([
    ['ambas as datas e página explícita', 5, '2026-01-01', '2026-01-31',
      '/api/ativos/4/cotacoes?pagina=5&tamanho=30&dataInicio=2026-01-01&dataFim=2026-01-31'],
    ['somente data inicial', 0, '2026-01-01', '',
      '/api/ativos/4/cotacoes?pagina=0&tamanho=30&dataInicio=2026-01-01'],
    ['somente data final', 0, '', '2026-01-31',
      '/api/ativos/4/cotacoes?pagina=0&tamanho=30&dataFim=2026-01-31'],
    ['sem datas usa página padrão', 0, '', '', '/api/ativos/4/cotacoes?pagina=0&tamanho=30'],
  ])('monta query de histórico com %s', async (_description, page, from, to, expectedUrl) => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ cotacoes: [], pagina: page, tamanho: 30, total: 0 }))

    await ativosApi.historico(4, from, to, page)

    expect(fetchMock).toHaveBeenCalledWith(expectedUrl, expect.objectContaining({
      headers: { 'Content-Type': 'application/json' },
    }))
  })

  it.each([
    ['errors de validação', { errors: ['quantidade: deve ser positiva', 'dataCompra: obrigatória'] },
      'quantidade: deve ser positiva dataCompra: obrigatória'],
    ['mensagem da API', { mensagem: 'Sincronização indisponível.' }, 'Sincronização indisponível.'],
    ['ProblemDetail detail', { detail: 'Ativo não encontrado.' }, 'Ativo não encontrado.'],
  ])('usa %s como mensagem de erro HTTP', async (_description, body, expectedMessage) => {
    fetchMock.mockResolvedValueOnce(jsonResponse(body, 400))

    await expect(ativosApi.listar()).rejects.toThrow(expectedMessage)
  })

  it('usa o status HTTP quando o corpo de erro não contém JSON', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: false,
      status: 502,
      json: vi.fn().mockRejectedValue(new SyntaxError('Unexpected token')),
    } as unknown as Response)

    await expect(ativosApi.sincronizar()).rejects.toThrow('Falha na solicitação (502).')
  })
})
