import { Link, Route, Routes } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import Search from './pages/Search'
import Add from './pages/Add'
import Lists from './pages/Lists'
import Settings from './pages/Settings'
import Trash from './pages/Trash'
import ItemDetail from './pages/ItemDetail'

export default function App() {
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
