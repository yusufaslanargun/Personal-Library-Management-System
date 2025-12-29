import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiGet, apiPost } from '../api'
import { Loan } from '../types'

type LoanFilter = 'ALL' | 'ACTIVE' | 'RETURNED' | 'REMOVED'

export default function Loans() {
  const [loans, setLoans] = useState<Loan[]>([])
  const [filter, setFilter] = useState<LoanFilter>('ACTIVE')
  const [error, setError] = useState('')

  const load = () => {
    const query = filter === 'ALL' ? '' : `?status=${filter}`
    apiGet<Loan[]>(`/loans${query}`)
      .then(setLoans)
      .catch((err) => setError(err.message))
  }

  useEffect(() => {
    load()
  }, [filter])

  const markReturned = async (loanId: number) => {
    try {
      await apiPost(`/loans/${loanId}/return`)
      load()
    } catch (err: any) {
      setError(err.message)
    }
  }

  const overdueIds = useMemo(() => {
    const today = new Date().toISOString().slice(0, 10)
    return new Set(
      loans
        .filter((loan) => loan.status === 'ACTIVE' && loan.dueDate < today)
        .map((loan) => loan.id)
    )
  }, [loans])

  return (
    <div className="stack">
      <div className="hero">
        <h1>Loans</h1>
        <p>Track who has your items and what is overdue.</p>
      </div>
      {error && <div className="banner error">{error}</div>}
      <div className="card">
        <div className="toolbar">
          <div className="tabs">
            {(['ALL', 'ACTIVE', 'RETURNED'] as LoanFilter[]).map((value) => (
              <button
                key={value}
                className={filter === value ? 'active' : ''}
                onClick={() => setFilter(value)}
              >
                {value}
              </button>
            ))}
          </div>
          <span className="pill">{loans.length} loans</span>
        </div>
        {loans.length === 0 ? (
          <p className="muted">No loans found.</p>
        ) : (
          <ul className="list">
            {loans.map((loan) => {
              const overdue = overdueIds.has(loan.id)
              return (
                <li key={loan.id}>
                  <div className="item-info">
                    <Link className="item-link" to={`/items/${loan.itemId}`}>
                      {loan.itemTitle || `Item #${loan.itemId}`}
                    </Link>
                    <div className="item-meta">
                      {loan.itemType && (
                        <span className={`pill small ${loan.itemType === 'BOOK' ? 'book' : 'dvd'}`}>
                          {loan.itemType}
                        </span>
                      )}
                      <span>To {loan.toWhom}</span>
                      <span>Start {loan.startDate}</span>
                      <span>Due {loan.dueDate}</span>
                      {overdue && <span className="status-badge overdue">OVERDUE</span>}
                    </div>
                  </div>
                  <div className="actions">
                    <span className={`status-badge ${loan.status === 'ACTIVE' ? 'loaned' : 'available'}`}>
                      {loan.status}
                    </span>
                    {loan.status === 'ACTIVE' && (
                      <button className="ghost small" onClick={() => markReturned(loan.id)}>
                        Mark Returned
                      </button>
                    )}
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </div>
  )
}
