export type Ativo = {
  id: number
  nomeTitulo: string
  dataCompra: string
  taxaContratada: number
  precoCompra: number
  quantidade: number
}

export type AtivoInput = Omit<Ativo, 'id'>

export type Posicao = {
  ativo: Ativo
  dataCotacao: string | null
  puVenda: number | null
  valorInvestido: number | null
  valorAtualEstimado: number | null
  variacaoValor: number | null
  variacaoPercentual: number | null
}

export type ResumoCarteira = {
  totalInvestido: number | null
  valorAtualEstimado: number | null
  variacaoValor: number | null
  variacaoPercentual: number | null
  cotacoesIndisponiveis: number
  posicoes: Posicao[]
}

export type Cotacao = {
  id: number
  nomeAtivo: string
  dataCotacao: string
  taxaAtual: number
  precoCompraAtual: number
  precoResgateAtual: number
}

export type Historico = {
  cotacoes: Cotacao[]
  pagina: number
  tamanho: number
  total: number
}

export type Alerta = {
  id: number
  ativoId: number
  nomeAtivo: string
  dataCotacao: string
  percentualObservado: number
  regraAplicada: string
  limitePercentual: number
  criadoEm: string
  lido: boolean
  lidoEm: string | null
}

export type AlertasRecentes = {
  alertas: Alerta[]
  pagina: number
  tamanho: number
  total: number
}

export type SincronizacaoStatus = {
  emAndamento: boolean
  iniciadaEm: string | null
  concluidaEm: string | null
  sucesso: boolean | null
  cotacoesPersistidas: number | null
  posicoesCorrespondentes: number | null
  mensagem: string
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  })

  if (!response.ok) {
    let message = `Falha na solicitação (${response.status}).`
    try {
      const problem = await response.json()
      const validationMessages = Array.isArray(problem.errors) ? problem.errors.join(' ') : undefined
      message = validationMessages ?? problem.mensagem ?? problem.detail ?? problem.title ?? message
    } catch {
      // Algumas respostas de erro nao incluem corpo JSON.
    }
    throw new Error(message)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const ativosApi = {
  statusSincronizacao: () => request<SincronizacaoStatus>('/api/sincronizacao'),
  sincronizar: () => request<SincronizacaoStatus>('/api/sincronizacao', { method: 'POST' }),
  listar: () => request<Ativo[]>('/api/ativos'),
  resumo: () => request<ResumoCarteira>('/api/ativos/resumo'),
  historico: (id: number, dataInicio: string, dataFim: string, pagina = 0) => {
    const params = new URLSearchParams()
    params.set('pagina', String(pagina))
    params.set('tamanho', '30')
    if (dataInicio) params.set('dataInicio', dataInicio)
    if (dataFim) params.set('dataFim', dataFim)
    const query = params.size ? `?${params.toString()}` : ''
    return request<Historico>(`/api/ativos/${id}/cotacoes${query}`)
  },
  alertas: () => request<AlertasRecentes>('/api/alertas?tamanho=20'),
  marcarAlertaComoLido: (id: number) => request<Alerta>(`/api/alertas/${id}/lido`, { method: 'PATCH' }),
  criar: (ativo: AtivoInput) => request<Ativo>('/api/ativos', {
    method: 'POST',
    body: JSON.stringify(ativo),
  }),
  atualizar: (id: number, ativo: AtivoInput) => request<Ativo>(`/api/ativos/${id}`, {
    method: 'PUT',
    body: JSON.stringify(ativo),
  }),
  remover: (id: number) => request<void>(`/api/ativos/${id}`, { method: 'DELETE' }),
}
