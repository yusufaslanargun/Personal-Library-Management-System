import { useEffect, useState } from 'react'
import { apiGet, apiPost, apiPut, apiDelete } from '../api'
import { MediaList } from '../types'

export default function Lists() {
  const [lists, setLists] = useState<MediaList[]>([])
  const [selected, setSelected] = useState<MediaList | null>(null)
  const [newName, setNewName] = useState('')
  const [addItemId, setAddItemId] = useState('')
  const [error, setError] = useState('')

  const load = () => {
    apiGet<MediaList[]>('/lists')
      .then((response) => {
        setLists(response)
        if (selected) {
          const updated = response.find((list) => list.id === selected.id)
          setSelected(updated || null)
        }
      })
      .catch((err) => setError(err.message))
  }

  useEffect(() => {
    load()
  }, [])

  const createList = async () => {
    if (!newName.trim()) return
    try {
      const list = await apiPost<MediaList>('/lists', { name: newName })
      setNewName('')
      setLists([...lists, list])
    } catch (err: any) {
      setError(err.message)
    }
  }

  const deleteList = async (id: number) => {
    const list = lists.find((entry) => entry.id === id)
    if (list && list.items.length > 0) {
      const confirmed = window.confirm(`Delete "${list.name}" with ${list.items.length} items?`)
      if (!confirmed) {
        return
      }
    }
    await apiDelete(`/lists/${id}`)
    setSelected(null)
    load()
  }

  const addItem = async () => {
    if (!selected || !addItemId) return
    await apiPost(`/lists/${selected.id}/items`, { itemId: Number(addItemId) })
    setAddItemId('')
    load()
  }

  const reorder = async (itemIds: number[]) => {
    if (!selected) return
    await apiPost(`/lists/${selected.id}/items/reorder`, { itemIds })
    load()
  }

  const move = (index: number, direction: number) => {
    if (!selected) return
    const items = [...selected.items]
    const targetIndex = index + direction
    if (targetIndex < 0 || targetIndex >= items.length) return
    ;[items[index], items[targetIndex]] = [items[targetIndex], items[index]]
    reorder(items.map((item) => item.itemId))
  }

  return (
    <div className="stack">
      <div className="hero">
        <h1>Lists</h1>
        <p>Curate your reading queues and collections.</p>
      </div>
      {error && <div className="banner error">{error}</div>}
      <div className="grid two">
        <div className="card">
          <h3>Create List</h3>
          <div className="inline">
            <input value={newName} onChange={(e) => setNewName(e.target.value)} placeholder="List name" />
            <button onClick={createList}>Create</button>
          </div>
          <ul className="list">
            {lists.map((list) => (
              <li key={list.id}>
                <button className="link" onClick={() => setSelected(list)}>{list.name}</button>
              </li>
            ))}
          </ul>
        </div>

        <div className="card">
          {selected ? (
            <>
              <div className="toolbar">
                <h3>{selected.name}</h3>
                <button onClick={() => deleteList(selected.id)}>Delete</button>
              </div>
              <div className="inline">
                <input value={addItemId} onChange={(e) => setAddItemId(e.target.value)} placeholder="Item ID" />
                <button onClick={addItem}>Add Item</button>
              </div>
              {selected.items.length === 0 ? (
                <p className="muted">No items yet.</p>
              ) : (
                <ul className="list">
                  {selected.items.map((item, index) => (
                    <li key={item.itemId}>
                      <span>{item.title}</span>
                      <div className="actions">
                        <button onClick={() => move(index, -1)}>Up</button>
                        <button onClick={() => move(index, 1)}>Down</button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </>
          ) : (
            <p className="muted">Select a list to manage items.</p>
          )}
        </div>
      </div>
    </div>
  )
}
