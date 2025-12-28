import { getToken } from './auth'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

function authHeaders() {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export async function apiGet<T>(path: string): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: {
      ...authHeaders()
    }
  })
  if (!res.ok) {
    throw new Error(await res.text())
  }
  return res.json()
}

export async function apiPost<T>(path: string, body?: unknown): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders()
    },
    body: body ? JSON.stringify(body) : undefined
  })
  if (!res.ok) {
    throw new Error(await res.text())
  }
  return res.json()
}

export async function apiPut<T>(path: string, body?: unknown): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders()
    },
    body: body ? JSON.stringify(body) : undefined
  })
  if (!res.ok) {
    throw new Error(await res.text())
  }
  return res.json()
}

export async function apiDelete(path: string): Promise<void> {
  const res = await fetch(`${API_BASE}${path}`, {
    method: 'DELETE',
    headers: {
      ...authHeaders()
    }
  })
  if (!res.ok) {
    throw new Error(await res.text())
  }
}

export async function apiDownload(path: string): Promise<Blob> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: {
      ...authHeaders()
    }
  })
  if (!res.ok) {
    throw new Error(await res.text())
  }
  return res.blob()
}

export async function apiUpload<T>(path: string, file: File): Promise<T> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: {
      ...authHeaders()
    },
    body: formData
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text)
  }
  return res.json()
}
