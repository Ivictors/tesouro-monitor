import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ativosApi, type Ativo, type AtivoInput } from './api/ativos'

const emptyForm: AtivoInput = {
  nomeTitulo: '',
  dataCompra: '',
  taxaContratada: 0,
  precoCompra: 0,
  quantidade: 0,
}

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export default function App() {
  const [ativos, setAtivos] = useState<Ativo[]>([])
  const [form, setForm] = useState<AtivoInput>(emptyForm)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setAtivos(await ativosApi.listar())
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível carregar a carteira.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { void load() }, [load])

  function update<K extends keyof AtivoInput>(field: K, value: AtivoInput[K]) {
    setForm(current => ({ ...current, [field]: value }))
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    try {
      if (editingId === null) await ativosApi.criar(form)
      else await ativosApi.atualizar(editingId, form)
      setForm(emptyForm)
      setEditingId(null)
      setNotice(editingId === null ? 'Ativo cadastrado.' : 'Ativo atualizado.')
      await load()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível salvar o ativo.')
    } finally {
      setSaving(false)
    }
  }

  function edit(ativo: Ativo) {
    setEditingId(ativo.id)
    setForm({
      nomeTitulo: ativo.nomeTitulo,
      dataCompra: ativo.dataCompra,
      taxaContratada: ativo.taxaContratada,
      precoCompra: ativo.precoCompra,
      quantidade: ativo.quantidade,
    })
    setNotice('')
  }

  async function remove(ativo: Ativo) {
    if (!window.confirm(`Remover ${ativo.nomeTitulo} da carteira?`)) return
    setError('')
    try {
      await ativosApi.remover(ativo.id)
      if (editingId === ativo.id) cancelEdit()
      setNotice('Ativo removido.')
      await load()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível remover o ativo.')
    }
  }

  function cancelEdit() {
    setForm(emptyForm)
    setEditingId(null)
  }

  return (
    <main className="shell">
      <header className="topbar">
        <a className="brand" href="/" aria-label="Tesouro Monitor, início">
          <span className="brand-mark" aria-hidden="true">T</span>
          <span>Tesouro<span className="brand-light">Monitor</span></span>
        </a>
        <span className="environment">CARTEIRA PESSOAL</span>
      </header>

      <section className="intro">
        <p className="eyebrow">ACOMPANHAMENTO DE RENDA FIXA</p>
        <h1>Sua carteira,<br /><em>em perspectiva.</em></h1>
        <p className="intro-copy">Cadastre seus títulos para acompanhar cotações e variações em um só lugar.</p>
      </section>

      <section className="content-grid" aria-label="Gestão da carteira">
        <section className="panel form-panel" aria-labelledby="form-title">
          <div className="panel-heading">
            <span className="step">01</span>
            <div>
              <p className="eyebrow">SUA POSIÇÃO</p>
              <h2 id="form-title">{editingId === null ? 'Adicionar título' : 'Editar título'}</h2>
            </div>
          </div>
          <form onSubmit={submit}>
            <label htmlFor="nomeTitulo">Título e vencimento</label>
            <input id="nomeTitulo" required maxLength={255} placeholder="Tesouro Selic - 2031-03-01"
              value={form.nomeTitulo} onChange={event => update('nomeTitulo', event.target.value)} />
            <p className="hint">Use o nome oficial seguido do vencimento (AAAA-MM-DD).</p>

            <div className="form-row">
              <div>
                <label htmlFor="dataCompra">Data da compra</label>
                <input id="dataCompra" type="date" required value={form.dataCompra}
                  onChange={event => update('dataCompra', event.target.value)} />
              </div>
              <div>
                <label htmlFor="taxaContratada">Taxa contratada (%)</label>
                <input id="taxaContratada" type="number" min="0" step="0.0001" required
                  value={form.taxaContratada || ''}
                  onChange={event => update('taxaContratada', Number(event.target.value))} />
              </div>
            </div>

            <div className="form-row">
              <div>
                <label htmlFor="precoCompra">Preço unitário de compra</label>
                <input id="precoCompra" type="number" min="0.01" step="0.01" required
                  value={form.precoCompra || ''}
                  onChange={event => update('precoCompra', Number(event.target.value))} />
              </div>
              <div>
                <label htmlFor="quantidade">Quantidade de títulos</label>
                <input id="quantidade" type="number" min="0.0001" step="0.0001" required
                  value={form.quantidade || ''}
                  onChange={event => update('quantidade', Number(event.target.value))} />
              </div>
            </div>

            {error && <p className="message error" role="alert">{error}</p>}
            {notice && <p className="message success" role="status">{notice}</p>}
            <div className="form-actions">
              {editingId !== null && <button className="button secondary" type="button" onClick={cancelEdit}>Cancelar</button>}
              <button className="button primary" type="submit" disabled={saving}>
                {saving ? 'Salvando…' : editingId === null ? 'Adicionar à carteira' : 'Salvar alterações'}
              </button>
            </div>
          </form>
        </section>

        <section className="panel portfolio-panel" aria-labelledby="portfolio-title">
          <div className="portfolio-heading">
            <div className="panel-heading">
              <span className="step">02</span>
              <div>
                <p className="eyebrow">VISÃO GERAL</p>
                <h2 id="portfolio-title">Ativos cadastrados</h2>
              </div>
            </div>
            <span className="count">{ativos.length.toString().padStart(2, '0')}</span>
          </div>

          {loading ? <p className="empty-state" role="status">Carregando carteira…</p> : null}
          {!loading && error && ativos.length === 0 ? <p className="empty-state">Não foi possível carregar seus ativos.</p> : null}
          {!loading && !error && ativos.length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon" aria-hidden="true">＋</span>
              <p>Sua carteira começa com o primeiro título.</p>
              <span>Preencha os dados ao lado para acompanhar sua posição.</span>
            </div>
          ) : null}
          {!loading && ativos.length > 0 && (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Título</th><th>Compra</th><th>Quantidade</th><th>Preço unitário</th><th><span className="sr-only">Ações</span></th></tr></thead>
                <tbody>
                  {ativos.map(ativo => (
                    <tr key={ativo.id}>
                      <td><strong>{ativo.nomeTitulo}</strong><span className="subcell">Taxa {ativo.taxaContratada}%</span></td>
                      <td>{new Date(`${ativo.dataCompra}T00:00:00`).toLocaleDateString('pt-BR')}</td>
                      <td>{ativo.quantidade}</td>
                      <td>{currency.format(ativo.precoCompra)}</td>
                      <td className="actions">
                        <button type="button" className="text-button" onClick={() => edit(ativo)}>Editar</button>
                        <button type="button" className="text-button danger-text" onClick={() => void remove(ativo)}>Remover</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <div className="panel-footnote">Cotações e variação aparecem quando a sincronização estiver concluída.</div>
        </section>
      </section>
      <footer>Dados informados por você. As cotações serão sincronizadas em dias úteis.</footer>
    </main>
  )
}
