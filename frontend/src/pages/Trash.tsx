import { useEffect, useState } from 'react'
import { apiGet, apiPost } from '../api'
import { Item } from '../types'

export default function Trash() {
  const [items, setItems] = useState<Item[]>([])
  const [error, setError] = useState('')

  const load = () => {
    apiGet<Item[]>('/items/trash')
      .then(setItems)
      .catch((err) => setError(err.message))
  }

  useEffect(() => {
    load()
  }, [])

  const restore = async (id: number) => {
    await apiPost(`/items/${id}/restore`)
    load()
  }

  return (
    <div className="stack">
      <div className="hero">
        <h1>Trash</h1>
        <p>Restore soft-deleted items.</p>
      </div>
      {error && <div className="banner error">{error}</div>}
      <div className="card">
        {items.length === 0 ? (
          <p className="muted">Trash is empty.</p>
        ) : (
          <ul className="list">
            {items.map((item) => (
              <li key={item.id}>
                <span>{item.title}</span>
                <button onClick={() => restore(item.id)}>Restore</button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
