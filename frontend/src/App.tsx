import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ativosApi, type Alerta, type Ativo, type AtivoInput, type Cotacao, type ResumoCarteira, type SincronizacaoStatus } from './api/ativos'

const emptyForm: AtivoInput = {
  nomeTitulo: '',
  dataCompra: '',
  taxaContratada: 0,
  precoCompra: 0,
  quantidade: 0,
}

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const percent = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

function money(value: number | null) {
  return value === null ? 'Indisponível' : currency.format(value)
}

function date(value: string | null) {
  return value ? new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR') : 'Sem cotação'
}

export default function App() {
  const [resumo, setResumo] = useState<ResumoCarteira | null>(null)
  const [alertas, setAlertas] = useState<Alerta[]>([])
  const [form, setForm] = useState<AtivoInput>(emptyForm)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [historico, setHistorico] = useState<Cotacao[]>([])
  const [historyPage, setHistoryPage] = useState(0)
  const [historyTotal, setHistoryTotal] = useState(0)
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [historyError, setHistoryError] = useState('')
  const [notice, setNotice] = useState('')
  const [syncStatus, setSyncStatus] = useState<SincronizacaoStatus | null>(null)
  const [syncRunning, setSyncRunning] = useState(false)
  const [syncError, setSyncError] = useState('')
  const [syncNotice, setSyncNotice] = useState('')

  const loadAlerts = useCallback(async () => {
    const page = await ativosApi.alertas()
    setAlertas(page.alertas)
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await ativosApi.resumo()
      setResumo(data)
      setSelectedId(current => current !== null && data.posicoes.some(position => position.ativo.id === current)
        ? current : data.posicoes[0]?.ativo.id ?? null)
      await loadAlerts()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível carregar o painel.')
    } finally {
      setLoading(false)
    }
  }, [loadAlerts])

  useEffect(() => { void load() }, [load])

  useEffect(() => {
    void ativosApi.statusSincronizacao().then(setSyncStatus).catch(() => {
      setSyncError('Não foi possível consultar o estado da sincronização.')
    })
  }, [])

  async function synchronizeQuotes() {
    setSyncRunning(true)
    setSyncError('')
    setSyncNotice('')
    try {
      const result = await ativosApi.sincronizar()
      setSyncStatus(result)
      if (result.sucesso) setSyncNotice(result.mensagem)
      else setSyncError(result.mensagem)
      await load()
    } catch (cause) {
      setSyncError(cause instanceof Error ? cause.message : 'Não foi possível atualizar as cotações.')
      try {
        setSyncStatus(await ativosApi.statusSincronizacao())
      } catch {
        // O erro da sincronização já é apresentado ao usuário.
      }
    } finally {
      setSyncRunning(false)
    }
  }

  const loadHistory = useCallback(async (id: number, start: string, end: string, page: number) => {
    setHistoryError('')
    try {
      const result = await ativosApi.historico(id, start, end, page)
      setHistorico(result.cotacoes)
      setHistoryPage(result.pagina)
      setHistoryTotal(result.total)
    } catch (cause) {
      setHistoryError(cause instanceof Error ? cause.message : 'Não foi possível carregar o histórico.')
      setHistorico([])
      setHistoryTotal(0)
    }
  }, [])

  useEffect(() => {
    if (selectedId !== null) void loadHistory(selectedId, '', '', 0)
    else {
      setHistorico([])
      setHistoryTotal(0)
    }
  }, [selectedId, loadHistory])

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

  async function markRead(alerta: Alerta) {
    try {
      await ativosApi.marcarAlertaComoLido(alerta.id)
      await loadAlerts()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível atualizar o alerta.')
    }
  }

  function cancelEdit() {
    setForm(emptyForm)
    setEditingId(null)
  }

  const positions = resumo?.posicoes ?? []
  const selected = positions.find(position => position.ativo.id === selectedId)

  return (
    <main className="shell">
      <header className="topbar">
        <a className="brand" href="/" aria-label="Tesouro Monitor, início">
          <span className="brand-mark" aria-hidden="true">T</span>
          <span>Tesouro<span className="brand-light">Monitor</span></span>
        </a>
        <nav className="main-nav" aria-label="Navegação principal">
          <a href="#inicio">Início</a>
          <a href="#carteira">Carteira</a>
          <a href="#historico">Histórico</a>
          <a href="#alertas">Alertas</a>
        </nav>
        <span className="environment">CARTEIRA PESSOAL · LOCAL</span>
      </header>

      <section className="intro" id="inicio">
        <p className="eyebrow">ACOMPANHAMENTO DE RENDA FIXA</p>
        <h1 aria-label="Bem-vindo ao Tesouro Monitor.">Bem-vindo ao<br /><em>Tesouro Monitor.</em></h1>
        <p className="intro-copy">Acompanhe sua carteira e a valorização estimada dos títulos com base no PU Venda divulgado pelo Tesouro Direto.</p>
        <div className="sync-controls">
          <button type="button" className="button primary" onClick={() => void synchronizeQuotes()} disabled={syncRunning}>
            {syncRunning ? 'Sincronizando…' : 'Sincronizar cotações'}
          </button>
          <p className="sync-meta" role="status">
            {syncRunning ? 'Buscando os dados mais recentes.' : syncStatus?.concluidaEm
              ? `${syncStatus.sucesso ? 'Última sincronização' : 'Última tentativa'}: ${new Date(syncStatus.concluidaEm).toLocaleString('pt-BR')}`
              : 'Nenhuma sincronização realizada nesta execução.'}
          </p>
        </div>
      </section>

      {error && <p className="message error" role="alert">{error}</p>}
      {syncError && <p className="message error" role="alert">{syncError}</p>}
      {syncNotice && <p className="message success" role="status">{syncNotice}</p>}

      <section className="summary-grid" aria-label="Resumo financeiro">
        <article className="summary-card"><span>Valor investido</span><strong>{money(resumo?.totalInvestido ?? null)}</strong></article>
        <article className="summary-card"><span>Valor atual estimado</span><strong>{money(resumo?.valorAtualEstimado ?? null)}</strong></article>
        <article className="summary-card"><span>Variação estimada</span><strong>{money(resumo?.variacaoValor ?? null)}</strong><small>{resumo?.variacaoPercentual === null || resumo?.variacaoPercentual === undefined ? 'Aguardando cotações' : `${percent.format(resumo.variacaoPercentual)}%`}</small></article>
      </section>
      {resumo && resumo.cotacoesIndisponiveis > 0 && (
        <p className="message warning" role="status">
          {resumo.cotacoesIndisponiveis} posição(ões) sem cálculo; o total atual só aparece quando todas têm cotação válida.
        </p>
      )}

      <section className="content-grid" id="carteira" aria-label="Gestão da carteira">
        <section className="panel form-panel" aria-labelledby="form-title">
          <div className="panel-heading"><span className="step">01</span><div><p className="eyebrow">SUA POSIÇÃO</p><h2 id="form-title">{editingId === null ? 'Adicionar título' : 'Editar título'}</h2></div></div>
          <form onSubmit={submit}>
            <label htmlFor="nomeTitulo">Título e vencimento</label>
            <input id="nomeTitulo" required maxLength={255} placeholder="Tesouro Selic - 2031-03-01"
              value={form.nomeTitulo} onChange={event => update('nomeTitulo', event.target.value)} />
            <p className="hint">Use o nome oficial seguido do vencimento (AAAA-MM-DD).</p>
            <div className="form-row">
              <div><label htmlFor="dataCompra">Data da compra</label><input id="dataCompra" type="date" required value={form.dataCompra} onChange={event => update('dataCompra', event.target.value)} /></div>
              <div><label htmlFor="taxaContratada">Taxa contratada (%)</label><input id="taxaContratada" type="number" min="0" step="0.0001" required value={form.taxaContratada || ''} onChange={event => update('taxaContratada', Number(event.target.value))} /></div>
            </div>
            <div className="form-row">
              <div><label htmlFor="precoCompra">Preço unitário de compra</label><input id="precoCompra" type="number" min="0.01" step="0.01" required value={form.precoCompra || ''} onChange={event => update('precoCompra', Number(event.target.value))} /></div>
              <div><label htmlFor="quantidade">Quantidade de títulos</label><input id="quantidade" type="number" min="0.0001" step="0.0001" required value={form.quantidade || ''} onChange={event => update('quantidade', Number(event.target.value))} /></div>
            </div>
            {notice && <p className="message success" role="status">{notice}</p>}
            <div className="form-actions">
              {editingId !== null && <button className="button secondary" type="button" onClick={cancelEdit}>Cancelar</button>}
              <button className="button primary" type="submit" disabled={saving}>{saving ? 'Salvando…' : editingId === null ? 'Adicionar à carteira' : 'Salvar alterações'}</button>
            </div>
          </form>
        </section>

        <section className="panel portfolio-panel" aria-labelledby="portfolio-title">
          <div className="portfolio-heading"><div className="panel-heading"><span className="step">02</span><div><p className="eyebrow">POSIÇÕES</p><h2 id="portfolio-title">Sua carteira</h2></div></div><span className="count">{positions.length.toString().padStart(2, '0')}</span></div>
          {loading && <p className="empty-state" role="status">Carregando carteira…</p>}
          {!loading && !error && positions.length === 0 && <div className="empty-state"><span className="empty-icon" aria-hidden="true">＋</span><p>Sua carteira começa com o primeiro título.</p><span>Preencha os dados ao lado para acompanhar sua posição.</span></div>}
          {!loading && positions.length > 0 && (
            <div className="table-wrap"><table>
              <thead><tr><th>Título</th><th>Investido</th><th>PU Venda</th><th>Variação</th><th>Ações</th></tr></thead>
              <tbody>{positions.map(position => <tr key={position.ativo.id}>
                <td><strong>{position.ativo.nomeTitulo}</strong><span className="subcell">Cotação de {date(position.dataCotacao)}</span></td>
                <td>{money(position.valorInvestido)}</td><td>{money(position.puVenda)}</td>
                <td>{position.variacaoPercentual === null ? 'Indisponível' : `${percent.format(position.variacaoPercentual)}%`}<span className="subcell">{money(position.variacaoValor)}</span></td>
                <td className="actions"><button type="button" className="text-button" onClick={() => setSelectedId(position.ativo.id)}>Histórico</button><button type="button" className="text-button" onClick={() => edit(position.ativo)}>Editar</button><button type="button" className="text-button danger-text" onClick={() => void remove(position.ativo)}>Remover</button></td>
              </tr>)}</tbody>
            </table></div>
          )}
          <div className="panel-footnote">A variação é estimada, bruta e não inclui impostos ou taxas.</div>
        </section>
      </section>

      <section className="panel detail-grid" aria-label="Histórico e alertas">
        <section id="historico" aria-labelledby="history-title">
          <div className="panel-heading"><span className="step">03</span><div><p className="eyebrow">COTAÇÕES</p><h2 id="history-title">Histórico do ativo</h2></div></div>
          {positions.length > 0 && <div className="history-filters">
            <label htmlFor="ativoHistorico">Ativo</label>
            <select id="ativoHistorico" value={selectedId ?? ''} onChange={event => setSelectedId(Number(event.target.value))}>{positions.map(position => <option key={position.ativo.id} value={position.ativo.id}>{position.ativo.nomeTitulo}</option>)}</select>
            <label htmlFor="dataInicio">De</label><input id="dataInicio" type="date" value={dataInicio} onChange={event => setDataInicio(event.target.value)} />
            <label htmlFor="dataFim">Até</label><input id="dataFim" type="date" value={dataFim} onChange={event => setDataFim(event.target.value)} />
            <button type="button" className="button secondary" onClick={() => selectedId !== null && void loadHistory(selectedId, dataInicio, dataFim, 0)}>Filtrar</button>
          </div>}
          {historyError && <p className="message error" role="alert">{historyError}</p>}
          {selected && historico.length === 0 && !historyError && <p className="empty-state">Nenhuma cotação encontrada para este período.</p>}
          {historico.length > 0 && <><div className="table-wrap"><table><thead><tr><th>Data</th><th>PU Compra</th><th>PU Venda</th><th>Taxa</th></tr></thead><tbody>{historico.map(quote => <tr key={quote.id}><td>{date(quote.dataCotacao)}</td><td>{money(quote.precoCompraAtual)}</td><td>{money(quote.precoResgateAtual)}</td><td>{percent.format(quote.taxaAtual)}%</td></tr>)}</tbody></table></div>
            <div className="pagination"><span>{historyTotal} cotações · página {historyPage + 1}</span><div><button type="button" className="button secondary" disabled={historyPage === 0} onClick={() => selectedId !== null && void loadHistory(selectedId, dataInicio, dataFim, historyPage - 1)}>Anterior</button><button type="button" className="button secondary" disabled={(historyPage + 1) * 30 >= historyTotal} onClick={() => selectedId !== null && void loadHistory(selectedId, dataInicio, dataFim, historyPage + 1)}>Próxima</button></div></div>
          </>}
        </section>

        <section id="alertas" aria-labelledby="alerts-title">
          <div className="panel-heading"><span className="step">04</span><div><p className="eyebrow">VALORIZAÇÃO ≥ 5%</p><h2 id="alerts-title">Alertas recentes</h2></div></div>
          {alertas.length === 0 && <p className="empty-state">Nenhum alerta de valorização até agora.</p>}
          {alertas.length > 0 && <ul className="alert-list">{alertas.map(alerta => <li className={alerta.lido ? 'alert-item read' : 'alert-item'} key={alerta.id}>
            <div><strong>{alerta.nomeAtivo}</strong><span className="subcell">{percent.format(alerta.percentualObservado)}% em {date(alerta.dataCotacao)}</span></div>
            {!alerta.lido && <button type="button" className="text-button" onClick={() => void markRead(alerta)}>Marcar como lido</button>}
            {alerta.lido && <span className="read-label">Lido</span>}
          </li>)}</ul>}
        </section>
      </section>
      <footer>Carteira pessoal disponível somente neste computador. Não exponha a aplicação publicamente.</footer>
    </main>
  )
}
