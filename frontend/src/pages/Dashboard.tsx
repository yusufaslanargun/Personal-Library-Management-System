import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiGet } from '../api'
import { Item, Loan } from '../types'

export default function Dashboard() {
  const [items, setItems] = useState<Item[]>([])
  const [overdue, setOverdue] = useState<Loan[]>([])
  const [error, setError] = useState('')

  const books = useMemo(() => items.filter((item) => item.type === 'BOOK'), [items])
  const dvds = useMemo(() => items.filter((item) => item.type === 'DVD'), [items])
  const loaned = useMemo(() => items.filter((item) => item.status === 'LOANED'), [items])

  useEffect(() => {
    Promise.all([apiGet<Item[]>('/items'), apiGet<Loan[]>('/loans/overdue')])
      .then(([itemsResponse, overdueResponse]) => {
        setItems(itemsResponse)
        setOverdue(overdueResponse)
      })
      .catch((err) => setError(err.message))
  }, [])

  return (
    <div className="stack">
      <div className="hero">
        <h1>Dashboard</h1>
        <p>Track your collection and what is due.</p>
      </div>
      {error && <div className="banner error">{error}</div>}
      <div className="grid">
        <div className="stat">
          <span>Total Items</span>
          <strong>{items.length}</strong>
        </div>
        <div className="stat">
          <span>Books</span>
          <strong>{books.length}</strong>
        </div>
        <div className="stat">
          <span>DVDs</span>
          <strong>{dvds.length}</strong>
        </div>
        <div className="stat">
          <span>Loaned</span>
          <strong>{loaned.length}</strong>
        </div>
      </div>
      <div className="grid two">
        <div className="card">
          <div className="toolbar">
            <h3>Books</h3>
            <span className="pill book">{books.length} total</span>
          </div>
          {books.length === 0 ? (
            <p className="muted">No books yet.</p>
          ) : (
            <ul className="list compact">
              {books.slice(0, 6).map((item) => (
                <li key={item.id}>
                  <div className="item-info">
                    <Link className="item-link" to={`/items/${item.id}`}>{item.title}</Link>
                    <div className="item-meta">
                      <span>{item.year}</span>
                    </div>
                  </div>
                  <span className={`status-badge ${item.status === 'LOANED' ? 'loaned' : 'available'}`}>{item.status}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
        <div className="card">
          <div className="toolbar">
            <h3>DVDs</h3>
            <span className="pill dvd">{dvds.length} total</span>
          </div>
          {dvds.length === 0 ? (
            <p className="muted">No DVDs yet.</p>
          ) : (
            <ul className="list compact">
              {dvds.slice(0, 6).map((item) => (
                <li key={item.id}>
                  <div className="item-info">
                    <Link className="item-link" to={`/items/${item.id}`}>{item.title}</Link>
                    <div className="item-meta">
                      <span>{item.year}</span>
                    </div>
                  </div>
                  <span className={`status-badge ${item.status === 'LOANED' ? 'loaned' : 'available'}`}>{item.status}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
      <div className="card">
        <div className="toolbar">
          <h3>Overdue Loans</h3>
          <span className="status-badge loaned">{overdue.length} overdue</span>
        </div>
        {overdue.length === 0 ? (
          <p className="muted">No overdue loans.</p>
        ) : (
          <ul className="list">
            {overdue.map((loan) => (
              <li key={loan.id}>
                <div className="item-info">
                  <span className="item-title">Item #{loan.itemId}</span>
                  <div className="item-meta">
                    <span>Due {loan.dueDate}</span>
                    <span>To {loan.toWhom}</span>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
