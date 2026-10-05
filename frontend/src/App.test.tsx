import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from './App'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('gestão da carteira', () => {
  const active = {
    id: 4,
    nomeTitulo: 'Tesouro Selic - 2031-03-01',
    dataCompra: '2026-01-10',
    taxaContratada: 0.1,
    precoCompra: 19943.12,
    quantidade: 0.25,
  }

  it('mostra o estado vazio e cadastra um título', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => [] })
      .mockResolvedValueOnce({ ok: true, status: 201, json: async () => ({ id: 4 }) })
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => [active] })
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)
    expect(await screen.findByText(/Sua carteira começa com o primeiro título/)).toBeInTheDocument()

    await user.type(screen.getByLabelText('Título e vencimento'), 'Tesouro Selic - 2031-03-01')
    await user.type(screen.getByLabelText('Data da compra'), '2026-01-10')
    await user.type(screen.getByLabelText('Taxa contratada (%)'), '0.1')
    await user.type(screen.getByLabelText('Preço unitário de compra'), '19943.12')
    await user.type(screen.getByLabelText('Quantidade de títulos'), '0.25')
    await user.click(screen.getByRole('button', { name: 'Adicionar à carteira' }))

    expect(await screen.findByText('Tesouro Selic - 2031-03-01')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/ativos', expect.objectContaining({ method: 'POST' }))
  })

  it('edita um ativo existente', async () => {
    const user = userEvent.setup()
    const updated = { ...active, quantidade: 0.5 }
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => [active] })
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => updated })
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => [updated] })
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)
    await user.click(await screen.findByRole('button', { name: 'Editar' }))
    const quantity = screen.getByLabelText('Quantidade de títulos')
    await user.clear(quantity)
    await user.type(quantity, '0.5')
    await user.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    expect(await screen.findByText('Ativo atualizado.')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/ativos/4', expect.objectContaining({ method: 'PUT' }))
  })

  it('pede confirmação antes de remover um ativo', async () => {
    const user = userEvent.setup()
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true)
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => [active] })
      .mockResolvedValueOnce({ ok: true, status: 204, json: async () => undefined })
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => [] })
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)
    await user.click(await screen.findByRole('button', { name: 'Remover' }))

    expect(confirm).toHaveBeenCalledWith(`Remover ${active.nomeTitulo} da carteira?`)
    expect(fetchMock).toHaveBeenCalledWith('/api/ativos/4', expect.objectContaining({ method: 'DELETE' }))
    expect(await screen.findByText(/Sua carteira começa com o primeiro título/)).toBeInTheDocument()
  })

  it('apresenta erro quando a API não está disponível', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('Backend indisponível')))
    render(<App />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Backend indisponível')
  })

  it('mostra mensagens de validação enviadas pela API', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => [] })
      .mockResolvedValueOnce({ ok: false, status: 400, json: async () => ({
        detail: 'Os dados enviados são inválidos.',
        errors: ['quantidade: deve ser positiva'],
      }) })
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)
    await screen.findByText(/Sua carteira começa com o primeiro título/)
    await user.type(screen.getByLabelText('Título e vencimento'), 'Tesouro Selic - 2031-03-01')
    await user.type(screen.getByLabelText('Data da compra'), '2026-01-10')
    await user.type(screen.getByLabelText('Taxa contratada (%)'), '0.1')
    await user.type(screen.getByLabelText('Preço unitário de compra'), '19943.12')
    await user.type(screen.getByLabelText('Quantidade de títulos'), '0.25')
    await user.click(screen.getByRole('button', { name: 'Adicionar à carteira' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('quantidade: deve ser positiva')
  })
})
