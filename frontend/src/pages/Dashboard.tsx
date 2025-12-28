import { useEffect, useState } from 'react'
import { apiGet } from '../api'
import { Item, Loan } from '../types'

export default function Dashboard() {
  const [items, setItems] = useState<Item[]>([])
  const [overdue, setOverdue] = useState<Loan[]>([])
  const [error, setError] = useState('')

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
          <span>Loaned</span>
          <strong>{items.filter((item) => item.status === 'LOANED').length}</strong>
        </div>
        <div className="stat">
          <span>Overdue</span>
          <strong>{overdue.length}</strong>
        </div>
      </div>
      <div className="card">
        <h3>Overdue Loans</h3>
        {overdue.length === 0 ? (
          <p className="muted">No overdue loans.</p>
        ) : (
          <ul className="list">
            {overdue.map((loan) => (
              <li key={loan.id}>
                Item #{loan.itemId} due {loan.dueDate} - {loan.toWhom}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
