import { Link, Route, Routes } from 'react-router-dom'
import { useEffect, useState } from 'react'
import Dashboard from './pages/Dashboard'
import Search from './pages/Search'
import Add from './pages/Add'
import Lists from './pages/Lists'
import Settings from './pages/Settings'
import Trash from './pages/Trash'
import ItemDetail from './pages/ItemDetail'
import Login from './pages/Login'
import { apiGet } from './api'
import { User } from './types'
import { clearToken, getToken } from './auth'

export default function App() {
  const [isAuthed, setIsAuthed] = useState(Boolean(getToken()))
  const [userName, setUserName] = useState('')

  useEffect(() => {
    if (!isAuthed) return
    apiGet<User>('/auth/me')
      .then((user) => setUserName(user.displayName))
      .catch(() => {
        clearToken()
        setIsAuthed(false)
      })
  }, [isAuthed])

  if (!isAuthed) {
    return <Login onAuthenticated={(name) => {
      setIsAuthed(true)
      setUserName(name)
    }} />
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">PLMS</div>
        <nav className="nav">
          <Link to="/">Dashboard</Link>
          <Link to="/search">Search</Link>
          <Link to="/add">Add</Link>
          <Link to="/lists">Lists</Link>
          <Link to="/settings">Settings</Link>
          <Link to="/trash">Trash</Link>
        </nav>
        <div className="nav">
          <span className="muted">{userName}</span>
          <button
            onClick={() => {
              clearToken()
              setIsAuthed(false)
              setUserName('')
            }}
          >
            Logout
          </button>
        </div>
      </header>
      <main className="main">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/search" element={<Search />} />
          <Route path="/add" element={<Add />} />
          <Route path="/items/:id" element={<ItemDetail />} />
          <Route path="/lists" element={<Lists />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="/trash" element={<Trash />} />
        </Routes>
      </main>
    </div>
  )
}
