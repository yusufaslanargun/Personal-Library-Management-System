import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { apiDelete, apiGet, apiPost, apiPut } from '../api'
import { Item, Loan, ProgressLog } from '../types'

interface DiffField {
  field: string
  currentValue?: string
  newValue?: string
}

export default function ItemDetail() {
  const { id } = useParams()
  const itemId = Number(id)
  const navigate = useNavigate()
  const [item, setItem] = useState<Item | null>(null)
  const [history, setHistory] = useState<ProgressLog[]>([])
  const [activeLoan, setActiveLoan] = useState<Loan | null>(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [diffs, setDiffs] = useState<DiffField[]>([])
  const [selectedFields, setSelectedFields] = useState<string[]>([])
  const [progressForm, setProgressForm] = useState({
    date: new Date().toISOString().slice(0, 10),
    pageOrMinute: ''
  })
  const [loanForm, setLoanForm] = useState({
    toWhom: '',
    startDate: new Date().toISOString().slice(0, 10),
    dueDate: ''
  })

  const tagsValue = useMemo(() => (item?.tags || []).join(', '), [item])

  const load = () => {
    if (!itemId) return
    Promise.all([
      apiGet<Item>(`/items/${itemId}`),
      apiGet<ProgressLog[]>(`/items/${itemId}/progress`),
      apiGet<Loan>(`/items/${itemId}/loan`).catch(() => null)
    ])
      .then(([itemResponse, historyResponse, loanResponse]) => {
        setItem(itemResponse)
        setHistory(historyResponse)
        setActiveLoan(loanResponse)
      })
      .catch((err) => setError(err.message))
  }

  useEffect(() => {
    load()
  }, [itemId])

  const saveItem = async () => {
    if (!item) return
    setError('')
    setSuccess('')
    try {
      const payload: any = {
        title: item.title,
        year: item.year,
        condition: item.condition || undefined,
        location: item.location || undefined,
        tags: item.tags,
        bookInfo: item.bookInfo,
        dvdInfo: item.dvdInfo
      }
      const updated = await apiPut<Item>(`/items/${item.id}`, payload)
      setItem(updated)
      
      setSuccess('Item details have been updated')
      
      setTimeout(() => setSuccess(''), 3000)

    } catch (err: any) {
      setError(err.message)
    }
  }

  const deleteItem = async () => {
    if (!item) return
    const confirmed = window.confirm(`"${item.title}" will be moved to Trash. Continue?`)
    if (!confirmed) return
    setError('')
    try {
      await apiDelete(`/items/${item.id}`)
      navigate('/trash')
    } catch (err: any) {
      setError(err.message)
    }
  }

  const refreshExternal = async () => {
    if (!item) return
    try {
      const response = await apiGet<DiffField[]>(`/items/${item.id}/external-refresh`)
      setDiffs(response)
      setSelectedFields(response.map((field) => field.field))
    } catch (err: any) {
      setError(err.message)
    }
  }

  const applyExternal = async () => {
    if (!item) return
    try {
      const updated = await apiPost<Item>(`/items/${item.id}/external-apply`, { fields: selectedFields })
      setItem(updated)
      setDiffs([])
    } catch (err: any) {
      setError(err.message)
    }
  }

  const logProgress = async () => {
    if (!item) return
    setError('')
    try {
      if (!activeLoan) {
        setError('Progress logging requires an active loan.')
        return
      }
      await apiPost(`/items/${item.id}/progress`, {
        date: progressForm.date,
        pageOrMinute: Number(progressForm.pageOrMinute)
      })
      await load()
      setProgressForm({ ...progressForm, pageOrMinute: '' })
    } catch (err: any) {
      setError(err.message)
    }
  }

  const deleteProgress = async (logId: number) => {
    if (!item) return
    setError('')
    try {
      await apiDelete(`/items/${item.id}/progress/${logId}`)
      await load()
    } catch (err: any) {
      setError(err.message)
    }
  }

  const createLoan = async () => {
    if (!item) return
    setError('')
    try {
      const loan = await apiPost<Loan>(`/items/${item.id}/loan`, loanForm)
      setActiveLoan(loan)
      await load()
    } catch (err: any) {
      setError(err.message)
    }
  }

  const returnLoan = async () => {
    if (!activeLoan) return
    try {
      await apiPost<Loan>(`/loans/${activeLoan.id}/return`)
      setActiveLoan(null)
      await load()
    } catch (err: any) {
      setError(err.message)
    }
  }

  if (!item) {
    return <div className="card">Loading...</div>
  }

  return (
    <div className="stack">
      <div className="hero">
        <div className="hero-head">
          <div>
            <h1>{item.title}</h1>
            <div className="item-meta">
              <span>ID #{item.id}</span>
              <span>Year {item.year}</span>
            </div>
          </div>
          <div className="hero-chips">
            <span className={`pill ${item.type === 'BOOK' ? 'book' : 'dvd'}`}>{item.type}</span>
            <span className={`status-badge ${item.status === 'LOANED' ? 'loaned' : 'available'}`}>{item.status}</span>
          </div>
        </div>
        <p className="muted">Progress {item.progressPercent}%</p>
        {activeLoan ? (
          <div className="loan-banner loaned">
            Loaned to {activeLoan.toWhom}. Due {activeLoan.dueDate}.
          </div>
        ) : (
          <div className="loan-banner available">Available for loan.</div>
        )}
      </div>
      {error && <div className="banner error">{error}</div>}
      {success && <div className="banner success">{success}</div>} {}

      <div className="grid two">
        <div className="card">
          <h3>Details</h3>
          <div className="form-grid">
            <label>
              Title
              <input value={item.title} onChange={(e) => setItem({ ...item, title: e.target.value })} />
            </label>
            <label>
              Year
              <input value={item.year} onChange={(e) => setItem({ ...item, year: Number(e.target.value) })} />
            </label>
            <label>
              Condition
              <input value={item.condition || ''} onChange={(e) => setItem({ ...item, condition: e.target.value })} />
            </label>
            <label>
              Location
              <input value={item.location || ''} onChange={(e) => setItem({ ...item, location: e.target.value })} />
            </label>
            <label>
              Tags
              <input
                value={tagsValue}
                onChange={(e) =>
                  setItem({
                    ...item,
                    tags: e.target.value.split(',').map((t) => t.trim()).filter(Boolean)
                  })
                }
              />
            </label>
          </div>
          {item.type === 'BOOK' && (
            <div className="form-grid">
              <label>
                ISBN
                <input value={item.bookInfo?.isbn || ''} onChange={(e) => setItem({
                  ...item,
                  bookInfo: { ...(item.bookInfo || {}), isbn: e.target.value }
                })} />
              </label>
              <label>
                Pages
                <input value={item.bookInfo?.pages || ''} onChange={(e) => setItem({
                  ...item,
                  bookInfo: { ...(item.bookInfo || {}), pages: Number(e.target.value) }
                })} />
              </label>
              <label>
                Publisher
                <input value={item.bookInfo?.publisher || ''} onChange={(e) => setItem({
                  ...item,
                  bookInfo: { ...(item.bookInfo || {}), publisher: e.target.value }
                })} />
              </label>
              <label>
                Authors
                <input value={item.bookInfo?.authors?.join(', ') || ''} onChange={(e) => setItem({
                  ...item,
                  bookInfo: { ...(item.bookInfo || {}), authors: e.target.value.split(',').map((t) => t.trim()).filter(Boolean) }
                })} />
              </label>
            </div>
          )}
          {item.type === 'DVD' && (
            <div className="form-grid">
              <label>
                Runtime
                <input value={item.dvdInfo?.runtime || ''} onChange={(e) => setItem({
                  ...item,
                  dvdInfo: { ...(item.dvdInfo || {}), runtime: Number(e.target.value) }
                })} />
              </label>
              <label>
                Director
                <input value={item.dvdInfo?.director || ''} onChange={(e) => setItem({
                  ...item,
                  dvdInfo: { ...(item.dvdInfo || {}), director: e.target.value }
                })} />
              </label>
              <label>
                Cast
                <input value={item.dvdInfo?.cast?.join(', ') || ''} onChange={(e) => setItem({
                  ...item,
                  dvdInfo: { ...(item.dvdInfo || {}), cast: e.target.value.split(',').map((t) => t.trim()).filter(Boolean) }
                })} />
              </label>
            </div>
          )}
          <div className="inline">
            <button onClick={saveItem}>Save</button>
            <button className="danger" onClick={deleteItem}>Move to Trash</button>
          </div>
          <p className="muted">Delete moves the item to Trash so it can be restored later.</p>
        </div>

        <div className="card">
          <h3>External Data</h3>
          <p className="muted">Sync metadata from providers.</p>
          <button className="secondary" onClick={refreshExternal}>Refresh</button>
          {diffs.length > 0 && (
            <div className="diffs">
              {diffs.map((diff) => (
                <label key={diff.field} className="diff-row">
                  <input
                    type="checkbox"
                    checked={selectedFields.includes(diff.field)}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedFields([...selectedFields, diff.field])
                      } else {
                        setSelectedFields(selectedFields.filter((field) => field !== diff.field))
                      }
                    }}
                  />
                  <div>
                    <strong>{diff.field}</strong>
                    <div className="muted">
                      {diff.currentValue || '-'} {'->'} {diff.newValue || '-'}
                    </div>
                  </div>
                </label>
              ))}
              <button onClick={applyExternal}>Apply Selected</button>
            </div>
          )}
        </div>
      </div>

      <div className="grid two">
        <div className="card">
          <h3>Progress</h3>
          <div className="progress">
            <span>{item.progressPercent}%</span>
            <div className="bar">
              <div style={{ width: `${item.progressPercent}%` }} />
            </div>
          </div>
          <div className="form-grid">
            <label>
              Date
              <input type="date" value={progressForm.date} onChange={(e) => setProgressForm({ ...progressForm, date: e.target.value })} />
            </label>
            <label>
              Page / Minute
              <input value={progressForm.pageOrMinute} onChange={(e) => setProgressForm({ ...progressForm, pageOrMinute: e.target.value })} />
            </label>
          </div>
          <button onClick={logProgress} disabled={!activeLoan}>Log Progress</button>
          {!activeLoan && (
            <p className="muted">Create a loan first to track progress.</p>
          )}
          <div className="divider" />
          <h4>History</h4>
          {history.length === 0 ? (
            <p className="muted">No logs yet.</p>
          ) : (
            <ul className="list">
              {history.map((log) => (
                <li key={log.id}>
                  <div className="item-info">
                    <span className="item-title">{log.date}</span>
                    <div className="item-meta">
                      <span>{log.pageOrMinute} ({log.percent}%)</span>
                      <span>{log.readerName || 'Reader unknown'}</span>
                    </div>
                  </div>
                  <div className="actions">
                    <button className="ghost small" onClick={() => deleteProgress(log.id)}>Delete</button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card">
          <div className="loan-header">
            <div>
              <h3>Loan Status</h3>
              <p className="muted">Track who has this item and when it is due.</p>
            </div>
            <span className={`status-badge ${activeLoan ? 'loaned' : 'available'}`}>
              {activeLoan ? 'LOANED' : 'AVAILABLE'}
            </span>
          </div>
          {activeLoan ? (
            <div className="stack">
              <div>
                Loaned to <strong>{activeLoan.toWhom}</strong>.
              </div>
              <div className="item-meta">
                <span>Start {activeLoan.startDate}</span>
                <span>Due {activeLoan.dueDate}</span>
              </div>
              <button className="secondary" onClick={returnLoan}>Mark Returned</button>
            </div>
          ) : (
            <div className="stack">
              <div className="form-grid">
                <label>
                  To Whom
                  <input value={loanForm.toWhom} onChange={(e) => setLoanForm({ ...loanForm, toWhom: e.target.value })} />
                </label>
                <label>
                  Start Date
                  <input type="date" value={loanForm.startDate} onChange={(e) => setLoanForm({ ...loanForm, startDate: e.target.value })} />
                </label>
                <label>
                  Due Date
                  <input type="date" value={loanForm.dueDate} onChange={(e) => setLoanForm({ ...loanForm, dueDate: e.target.value })} />
                </label>
              </div>
              <button onClick={createLoan}>Mark as Loaned</button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
