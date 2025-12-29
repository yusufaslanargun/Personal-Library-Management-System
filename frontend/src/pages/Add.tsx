import { useState } from 'react'
import { apiGet, apiPost } from '../api'
import { ExternalCandidate, Item } from '../types'

export default function Add() {
  const [isbn, setIsbn] = useState('')
  const [candidates, setCandidates] = useState<ExternalCandidate[]>([])
  const [isbnError, setIsbnError] = useState('')
  const [manualType, setManualType] = useState<'BOOK' | 'DVD'>('BOOK')
  const [manual, setManual] = useState({
    title: '',
    year: '',
    condition: '',
    location: '',
    tags: '',
    isbn: '',
    pages: '',
    publisher: '',
    authors: '',
    runtime: '',
    director: '',
    cast: ''
  })
  const [created, setCreated] = useState<Item | null>(null)
  const [manualError, setManualError] = useState('')

  const providerLabel = (provider: string) => {
    if (provider === 'OPEN_LIBRARY') return 'Open Library'
    if (provider === 'OMDB') return 'OMDb'
    return provider
  }

  const lookup = async () => {
    setIsbnError('')
    setCandidates([])
    try {
      const response = await apiGet<ExternalCandidate[]>(`/external/books/lookup?isbn=${isbn}`)
      setCandidates(response)
    } catch (err: any) {
      setIsbnError(err.message)
    }
  }

  const confirmCandidate = async (candidate: ExternalCandidate) => {
    setIsbnError('')
    try {
      const response = await apiPost<Item>('/external/books/confirm', {
        provider: candidate.provider,
        externalId: candidate.externalId,
        title: candidate.title,
        authors: candidate.authors,
        publisher: candidate.publisher,
        pageCount: candidate.pageCount,
        year: candidate.year,
        description: candidate.description,
        infoLink: candidate.infoLink,
        averageRating: candidate.averageRating,
        isbn
      })
      setCreated(response)
    } catch (err: any) {
      setIsbnError(err.message)
    }
  }

  const createManual = async () => {
    setManualError('')
    try {
      const payload: any = {
        type: manualType,
        title: manual.title,
        year: Number(manual.year),
        condition: manual.condition || undefined,
        location: manual.location || undefined,
        tags: manual.tags ? manual.tags.split(',').map((t) => t.trim()).filter(Boolean) : []
      }
      if (manualType === 'BOOK') {
        payload.bookInfo = {
          isbn: manual.isbn || undefined,
          pages: manual.pages ? Number(manual.pages) : undefined,
          publisher: manual.publisher || undefined,
          authors: manual.authors ? manual.authors.split(',').map((t) => t.trim()).filter(Boolean) : []
        }
      } else {
        payload.dvdInfo = {
          runtime: manual.runtime ? Number(manual.runtime) : undefined,
          director: manual.director || undefined,
          cast: manual.cast ? manual.cast.split(',').map((t) => t.trim()).filter(Boolean) : []
        }
      }
      const response = await apiPost<Item>('/items', payload)
      setCreated(response)
    } catch (err: any) {
      setManualError(err.message)
    }
  }

  return (
    <div className="stack">
      <div className="hero">
        <h1>Add to Library</h1>
        <p>Fast ISBN lookup or a manual entry.</p>
      </div>

      <div className="grid two">
        <div className="card">
          <h3>ISBN Lookup (Open Library)</h3>
          <div className="inline">
            <input value={isbn} onChange={(e) => setIsbn(e.target.value)} placeholder="ISBN or barcode" />
            <button className="secondary" onClick={lookup}>Lookup</button>
          </div>
          {isbnError && <div className="banner error">{isbnError}</div>}
          {candidates.length === 0 ? (
            <p className="muted">Enter a valid ISBN to fetch candidates.</p>
          ) : (
            <div className="candidate-list">
              {candidates.map((candidate) => (
                <div className="candidate" key={`${candidate.provider}-${candidate.externalId}`}>
                  <div>
                    <h4>{candidate.title}</h4>
                    <p className="muted">{candidate.authors?.join(', ')}</p>
                    <span className="pill">{providerLabel(candidate.provider)}</span>
                  </div>
                  <button onClick={() => confirmCandidate(candidate)}>Use</button>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="card">
          <h3>Manual Entry</h3>
          {manualError && <div className="banner error">{manualError}</div>}
          <div className="form-grid">
            <label>
              Type
              <select value={manualType} onChange={(e) => setManualType(e.target.value as 'BOOK' | 'DVD')}>
                <option value="BOOK">Book</option>
                <option value="DVD">DVD</option>
              </select>
            </label>
            <label>
              Title
              <input value={manual.title} onChange={(e) => setManual({ ...manual, title: e.target.value })} />
            </label>
            <label>
              Year
              <input value={manual.year} onChange={(e) => setManual({ ...manual, year: e.target.value })} />
            </label>
            <label>
              Condition
              <input value={manual.condition} onChange={(e) => setManual({ ...manual, condition: e.target.value })} />
            </label>
            <label>
              Location
              <input value={manual.location} onChange={(e) => setManual({ ...manual, location: e.target.value })} />
            </label>
            <label>
              Tags
              <input value={manual.tags} onChange={(e) => setManual({ ...manual, tags: e.target.value })} />
            </label>
            {manualType === 'BOOK' ? (
              <>
                <label>
                  ISBN
                  <input value={manual.isbn} onChange={(e) => setManual({ ...manual, isbn: e.target.value })} />
                </label>
                <label>
                  Pages
                  <input value={manual.pages} onChange={(e) => setManual({ ...manual, pages: e.target.value })} />
                </label>
                <label>
                  Publisher
                  <input value={manual.publisher} onChange={(e) => setManual({ ...manual, publisher: e.target.value })} />
                </label>
                <label>
                  Authors
                  <input value={manual.authors} onChange={(e) => setManual({ ...manual, authors: e.target.value })} />
                </label>
              </>
            ) : (
              <>
                <label>
                  Runtime (min)
                  <input value={manual.runtime} onChange={(e) => setManual({ ...manual, runtime: e.target.value })} />
                </label>
                <label>
                  Director
                  <input value={manual.director} onChange={(e) => setManual({ ...manual, director: e.target.value })} />
                </label>
                <label>
                  Cast
                  <input value={manual.cast} onChange={(e) => setManual({ ...manual, cast: e.target.value })} />
                </label>
              </>
            )}
          </div>
          <button onClick={createManual}>Create Item</button>
        </div>
      </div>

      {created && (
        <div className="banner success">
          Created: {created.title} (#{created.id})
        </div>
      )}
    </div>
  )
}
