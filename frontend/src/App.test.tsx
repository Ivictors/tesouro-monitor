import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from './App'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

const active = {
  id: 4,
  nomeTitulo: 'Tesouro Selic - 2031-03-01',
  dataCompra: '2026-01-10',
  taxaContratada: 0.1,
  precoCompra: 100,
  quantidade: 2,
}

const position = {
  ativo: active,
  dataCotacao: '2026-10-02',
  puVenda: 105,
  valorInvestido: 200,
  valorAtualEstimado: 210,
  variacaoValor: 10,
  variacaoPercentual: 5,
}

const emptySummary = {
  totalInvestido: 0,
  valorAtualEstimado: 0,
  variacaoValor: 0,
  variacaoPercentual: null,
  cotacoesIndisponiveis: 0,
  posicoes: [],
}

const populatedSummary = {
  totalInvestido: 200,
  valorAtualEstimado: 210,
  variacaoValor: 10,
  variacaoPercentual: 5,
  cotacoesIndisponiveis: 0,
  posicoes: [position],
}

const quote = {
  id: 9,
  nomeAtivo: active.nomeTitulo,
  dataCotacao: '2026-10-02',
  taxaAtual: 0.1,
  precoCompraAtual: 104,
  precoResgateAtual: 105,
}

const alert = {
  id: 7,
  ativoId: 4,
  nomeAtivo: active.nomeTitulo,
  dataCotacao: '2026-10-02',
  percentualObservado: 5,
  limitePercentual: 5,
  criadoEm: '2026-10-02T10:15:00',
  lido: false,
  lidoEm: null,
}

function response(data: unknown, status = 200) {
  return { ok: status >= 200 && status < 300, status, json: async () => data }
}

function setupApi(options: {
  summaries?: object[]
  alerts?: object[]
  failSummary?: boolean
} = {}) {
  const summaries = [...(options.summaries ?? [emptySummary])]
  let currentAlerts = options.alerts?.[0] ?? { alertas: [], pagina: 0, tamanho: 20, total: 0 }
  const fetchMock = vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
    const url = String(input)
    if (url === '/api/ativos/resumo') {
      if (options.failSummary) throw new Error('Backend indisponível')
      return response(summaries.shift() ?? emptySummary)
    }
    if (url.startsWith('/api/alertas')) {
      if (url.includes('/lido')) {
        const readAlert = { ...alert, lido: true, lidoEm: '2026-10-02T11:00:00' }
        currentAlerts = { alertas: [readAlert], pagina: 0, tamanho: 20, total: 1 }
        return response(readAlert)
      }
      return response(currentAlerts)
    }
    if (url.startsWith('/api/ativos/') && url.includes('/cotacoes')) {
      return response({ cotacoes: [quote], pagina: 0, tamanho: 30, total: 31 })
    }
    if (url === '/api/ativos' && init?.method === 'POST') return response(active, 201)
    if (url === `/api/ativos/${active.id}` && init?.method === 'PUT') return response({ ...active, quantidade: 3 })
    if (url === `/api/ativos/${active.id}` && init?.method === 'DELETE') return response(undefined, 204)
    return response([])
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

describe('painel e gestão da carteira', () => {
  it('mostra estado vazio e cadastra um título', async () => {
    const user = userEvent.setup()
    const fetchMock = setupApi({ summaries: [emptySummary, { ...emptySummary, posicoes: [position] }] })
    render(<App />)
    expect(await screen.findByText(/Sua carteira começa com o primeiro título/)).toBeInTheDocument()

    await user.type(screen.getByLabelText('Título e vencimento'), active.nomeTitulo)
    await user.type(screen.getByLabelText('Data da compra'), active.dataCompra)
    await user.type(screen.getByLabelText('Taxa contratada (%)'), '0.1')
    await user.type(screen.getByLabelText('Preço unitário de compra'), '100')
    await user.type(screen.getByLabelText('Quantidade de títulos'), '2')
    await user.click(screen.getByRole('button', { name: 'Adicionar à carteira' }))

    expect(await screen.findByText('Ativo cadastrado.')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/ativos', expect.objectContaining({ method: 'POST' }))
    expect((await screen.findAllByText(active.nomeTitulo)).length).toBeGreaterThan(0)
  })

  it('mostra totais, histórico filtrável e permite marcar alerta como lido', async () => {
    const user = userEvent.setup()
    const fetchMock = setupApi({
      summaries: [populatedSummary],
      alerts: [{ alertas: [alert], pagina: 0, tamanho: 20, total: 1 }],
    })
    render(<App />)

    expect(await screen.findByText(/210,00/)).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Alertas recentes' })).toBeInTheDocument()
    expect((await screen.findAllByText(active.nomeTitulo)).length).toBeGreaterThan(0)
    await waitFor(() => expect(screen.getAllByRole('cell', { name: /105,00/ }).length).toBeGreaterThanOrEqual(2))

    await user.type(screen.getByLabelText('De'), '2026-10-01')
    await user.type(screen.getByLabelText('Até'), '2026-10-31')
    await user.click(screen.getByRole('button', { name: 'Filtrar' }))
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      '/api/ativos/4/cotacoes?pagina=0&tamanho=30&dataInicio=2026-10-01&dataFim=2026-10-31', expect.objectContaining({ headers: expect.any(Object) })))
    await user.click(screen.getByRole('button', { name: 'Próxima' }))
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      '/api/ativos/4/cotacoes?pagina=1&tamanho=30&dataInicio=2026-10-01&dataFim=2026-10-31', expect.objectContaining({ headers: expect.any(Object) })))

    await user.click(screen.getByRole('button', { name: 'Marcar como lido' }))
    expect(await screen.findByText('Lido')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/alertas/7/lido', expect.objectContaining({ method: 'PATCH' }))
  })

  it('edita um ativo existente', async () => {
    const user = userEvent.setup()
    const fetchMock = setupApi({ summaries: [populatedSummary, { ...populatedSummary, posicoes: [{ ...position, ativo: { ...active, quantidade: 3 } }] }] })
    render(<App />)
    await user.click(await screen.findByRole('button', { name: 'Editar' }))
    const quantity = screen.getByLabelText('Quantidade de títulos')
    await user.clear(quantity)
    await user.type(quantity, '3')
    await user.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    expect(await screen.findByText('Ativo atualizado.')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/ativos/4', expect.objectContaining({ method: 'PUT' }))
  })

  it('pede confirmação antes de remover um ativo', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const fetchMock = setupApi({ summaries: [populatedSummary, emptySummary] })
    render(<App />)
    await user.click(await screen.findByRole('button', { name: 'Remover' }))

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith('/api/ativos/4', expect.objectContaining({ method: 'DELETE' })))
    expect(await screen.findByText(/Sua carteira começa com o primeiro título/)).toBeInTheDocument()
  })

  it('explica quando a API não está disponível', async () => {
    setupApi({ failSummary: true })
    render(<App />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Backend indisponível')
  })

  it('mostra a ausência de cotação sem inventar totais atuais', async () => {
    setupApi({ summaries: [{
      ...populatedSummary,
      valorAtualEstimado: null,
      variacaoValor: null,
      variacaoPercentual: null,
      cotacoesIndisponiveis: 1,
      posicoes: [{ ...position, dataCotacao: null, puVenda: null, valorAtualEstimado: null, variacaoValor: null, variacaoPercentual: null }],
    }] })
    render(<App />)
    expect(await screen.findByText(/1 posição\(ões\) sem cálculo/)).toBeInTheDocument()
    expect(screen.getAllByText('Indisponível').length).toBeGreaterThan(0)
  })
})
