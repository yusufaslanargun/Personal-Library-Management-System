import { useState } from 'react'
import { apiPost } from '../api'
import { AuthResponse } from '../types'
import { setToken } from '../auth'

interface LoginProps {
  onAuthenticated: (userName: string) => void
}

export default function Login({ onAuthenticated }: LoginProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [error, setError] = useState('')
  const [mode, setMode] = useState<'login' | 'register'>('login')

  const handleLogin = async () => {
    setError('')
    try {
      const response = await apiPost<AuthResponse>('/auth/login', { email, password })
      setToken(response.token)
      onAuthenticated(response.user.displayName)
    } catch (err: any) {
      setError(err.message)
    }
  }

  const handleRegister = async () => {
    setError('')
    try {
      const response = await apiPost<AuthResponse>('/auth/register', { email, password, displayName })
      setToken(response.token)
      onAuthenticated(response.user.displayName)
    } catch (err: any) {
      setError(err.message)
    }
  }

  return (
    <div className="auth">
      <div className="card auth-card">
        <h1>Welcome to PLMS</h1>
        <p className="muted">Sign in to manage your library.</p>
        {error && <div className="banner error">{error}</div>}
        <div className="tabs">
          <button className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>Login</button>
          <button className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>Register</button>
        </div>
        <div className="form-grid">
          <label>
            Email
            <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" />
          </label>
          <label>
            Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
          </label>
          {mode === 'register' && (
            <label>
              Display Name
              <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="Your name" />
            </label>
          )}
        </div>
        {mode === 'login' ? (
          <button onClick={handleLogin}>Login</button>
        ) : (
          <button onClick={handleRegister}>Create Account</button>
        )}
      </div>
    </div>
  )
}
