export type Ativo = {
  id: number
  nomeTitulo: string
  dataCompra: string
  taxaContratada: number
  precoCompra: number
  quantidade: number
}

export type AtivoInput = Omit<Ativo, 'id'>

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
      message = validationMessages ?? problem.detail ?? problem.title ?? message
    } catch {
      // Algumas respostas de erro nao incluem corpo JSON.
    }
    throw new Error(message)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const ativosApi = {
  listar: () => request<Ativo[]>('/api/ativos'),
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
