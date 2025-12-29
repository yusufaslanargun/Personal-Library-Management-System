import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiGet } from '../api'
import { Item, SearchResponse } from '../types'

export default function Search() {
  const [query, setQuery] = useState('')
  const [type, setType] = useState('')
  const [status, setStatus] = useState('')
  const [tags, setTags] = useState('')
  const [author, setAuthor] = useState('')
  const [cast, setCast] = useState('')
  const [year, setYear] = useState('')
  const [condition, setCondition] = useState('')
  const [location, setLocation] = useState('')
  const [page, setPage] = useState(0)
  const [results, setResults] = useState<Item[]>([])
  const [total, setTotal] = useState(0)
  const [error, setError] = useState('')

  const queryString = useMemo(() => {
    const params = new URLSearchParams()
    if (query) params.set('query', query)
    if (type) params.set('type', type)
    if (status) params.set('status', status)
    if (year) params.set('year', year)
    if (condition) params.set('condition', condition)
    if (location) params.set('location', location)
    if (author) params.set('author', author)
    if (cast) params.set('cast', cast)
    const tagList = tags.split(',').map((t) => t.trim()).filter(Boolean)
    tagList.forEach((tag) => params.append('tags', tag))
    params.set('page', String(page))
    params.set('size', '12')
    return params.toString()
  }, [query, type, status, tags, author, cast, year, condition, location, page])

  useEffect(() => {
    apiGet<SearchResponse>(`/items/search?${queryString}`)
      .then((response) => {
        setResults(response.items)
        setTotal(response.total)
      })
      .catch((err) => setError(err.message))
  }, [queryString])

  useEffect(() => {
    setPage(0)
  }, [query, type, status, tags, author, cast, year, condition, location])

  return (
    <div className="stack">
      <div className="hero">
        <h1>Search Library</h1>
        <p>Search across title, tags, and metadata.</p>
      </div>
      {error && <div className="banner error">{error}</div>}
      <div className="card">
        <div className="form-grid">
          <label>
            Query
            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Title, author, keyword" />
          </label>
          <label>
            Type
            <select value={type} onChange={(e) => setType(e.target.value)}>
              <option value="">All</option>
              <option value="BOOK">Book</option>
              <option value="DVD">DVD</option>
            </select>
          </label>
          <label>
            Status
            <select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">All</option>
              <option value="AVAILABLE">Available</option>
              <option value="LOANED">Loaned</option>
            </select>
          </label>
          <label>
            Tags (comma)
            <input value={tags} onChange={(e) => setTags(e.target.value)} placeholder="classic, sci-fi" />
          </label>
          <label>
            Author
            <input value={author} onChange={(e) => setAuthor(e.target.value)} placeholder="Ursula Le Guin" />
          </label>
          <label>
            Cast
            <input value={cast} onChange={(e) => setCast(e.target.value)} placeholder="Tilda Swinton" />
          </label>
          <label>
            Year
            <input value={year} onChange={(e) => setYear(e.target.value)} placeholder="2024" />
          </label>
          <label>
            Condition
            <input value={condition} onChange={(e) => setCondition(e.target.value)} placeholder="New" />
          </label>
          <label>
            Location
            <input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="Shelf A" />
          </label>
        </div>
      </div>

      <div className="card">
        <div className="toolbar">
          <span>{total} results</span>
          <div className="pager">
            <button className="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Prev
            </button>
            <button className="secondary" disabled={(page + 1) * 12 >= total} onClick={() => setPage(page + 1)}>
              Next
            </button>
          </div>
        </div>
        {results.length === 0 ? (
          <p className="muted">No results yet.</p>
        ) : (
          <div className="results">
            {results.map((item) => (
              <Link to={`/items/${item.id}`} key={item.id} className="result-card">
                <div className="result-top">
                  <span className="pill">{item.type}</span>
                  <span className="status">{item.status}</span>
                </div>
                <h4>{item.title}</h4>
                <p className="muted">{item.year}</p>
                <div className="tag-row">
                  {item.tags.slice(0, 3).map((tag) => (
                    <span key={tag} className="tag">{tag}</span>
                  ))}
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
