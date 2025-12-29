import { NavLink, Route, Routes } from 'react-router-dom'
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
  const [drawerOpen, setDrawerOpen] = useState(false)

  const navItems = [
    { to: '/', label: 'Dashboard', end: true },
    { to: '/search', label: 'Search' },
    { to: '/add', label: 'Add' },
    { to: '/lists', label: 'Lists' },
    { to: '/settings', label: 'Settings' },
    { to: '/trash', label: 'Trash' }
  ]

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

  const userInitial = userName ? userName[0].toUpperCase() : 'U'
  const logout = () => {
    clearToken()
    setIsAuthed(false)
    setUserName('')
    setDrawerOpen(false)
  }
  const navLinks = navItems.map((item) => (
    <NavLink
      key={item.to}
      to={item.to}
      end={item.end}
      className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
      onClick={() => setDrawerOpen(false)}
    >
      <span className="nav-dot" />
      {item.label}
    </NavLink>
  ))

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-header">
          <div className="brand">PLMS</div>
          <span className="badge">Personal Library</span>
        </div>
        <nav className="side-nav">
          {navLinks}
        </nav>
        <div className="sidebar-footer">
          <div className="user-chip">
            <span className="avatar">{userInitial}</span>
            <div>
              <div className="user-name">{userName || 'Reader'}</div>
              <div className="user-meta">Signed in</div>
            </div>
          </div>
          <button className="ghost" onClick={logout}>Logout</button>
        </div>
      </aside>

      <div className="app-main">
        <header className="topbar">
          <button
            className="ghost menu-button"
            aria-label="Toggle navigation"
            aria-expanded={drawerOpen}
            aria-controls="mobile-menu"
            onClick={() => setDrawerOpen((open) => !open)}
          >
            Menu
          </button>
          <div className="topbar-title">
            <span className="brand">PLMS</span>
            <span className="badge">Library</span>
          </div>
          <button className="ghost" onClick={logout}>Logout</button>
        </header>

        <div className={`mobile-drawer ${drawerOpen ? 'open' : ''}`} id="mobile-menu" aria-hidden={!drawerOpen}>
          <div className="drawer-header">
            <div>
              <div className="brand">PLMS</div>
              <span className="muted">Personal Library</span>
            </div>
            <button className="ghost small" onClick={() => setDrawerOpen(false)}>Close</button>
          </div>
          <nav className="side-nav">
            {navLinks}
          </nav>
          <div className="sidebar-footer">
            <div className="user-chip">
              <span className="avatar">{userInitial}</span>
              <div>
                <div className="user-name">{userName || 'Reader'}</div>
                <div className="user-meta">Signed in</div>
              </div>
            </div>
            <button className="ghost" onClick={logout}>Logout</button>
          </div>
        </div>
        {drawerOpen && <div className="backdrop" onClick={() => setDrawerOpen(false)} />}

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
    </div>
  )
}
