import { useEffect, useState } from 'react'
import { apiDownload, apiGet, apiPost, apiUpload } from '../api'
import { ImportSummary, SyncStatus } from '../types'

export default function Settings() {
  const [importFormat, setImportFormat] = useState<'json' | 'csv'>('json')
  const [exportFormat, setExportFormat] = useState<'json' | 'csv'>('json')
  const [summary, setSummary] = useState<ImportSummary | null>(null)
  const [syncStatus, setSyncStatus] = useState<SyncStatus | null>(null)
  const [error, setError] = useState('')

  const loadSync = () => {
    apiGet<SyncStatus>('/sync/status').then(setSyncStatus).catch(() => null)
  }

  useEffect(() => {
    loadSync()
  }, [])

  const exportData = async () => {
    setError('')
    try {
      const blob = await apiDownload(`/export?format=${exportFormat}`)
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = exportFormat === 'csv' ? 'plms-export.zip' : 'plms-export.json'
      link.click()
      window.URL.revokeObjectURL(url)
    } catch (err: any) {
      setError(err.message)
    }
  }

  const importData = async (file: File | null) => {
    if (!file) return
    setError('')
    try {
      const response = await apiUpload<ImportSummary>(`/import?format=${importFormat}`, file)
      setSummary(response)
    } catch (err: any) {
      setError(err.message)
    }
  }

  const toggleSync = async (enabled: boolean) => {
    await apiPost<SyncStatus>('/sync/enable', { enabled })
    loadSync()
  }

  const runSync = async () => {
    await apiPost<SyncStatus>('/sync/run')
    loadSync()
  }

  return (
    <div className="stack">
      <div className="hero">
        <h1>Settings</h1>
        <p>Import/export your data and manage sync.</p>
      </div>
      {error && <div className="banner error">{error}</div>}
      <div className="grid two">
        <div className="card">
          <h3>Export</h3>
          <div className="inline">
            <select value={exportFormat} onChange={(e) => setExportFormat(e.target.value as 'json' | 'csv')}>
              <option value="json">JSON</option>
              <option value="csv">CSV (zip)</option>
            </select>
            <button onClick={exportData}>Download</button>
          </div>
        </div>
        <div className="card">
          <h3>Import</h3>
          <div className="inline">
            <select value={importFormat} onChange={(e) => setImportFormat(e.target.value as 'json' | 'csv')}>
              <option value="json">JSON</option>
              <option value="csv">CSV (zip)</option>
            </select>
            <input type="file" onChange={(e) => importData(e.target.files?.[0] || null)} />
          </div>
          {summary && (
            <div className="summary">
              <p>Added: {summary.added}, Updated: {summary.updated}, Skipped: {summary.skipped}</p>
              {summary.errors?.length > 0 && (
                <ul className="list">
                  {summary.errors.map((err, idx) => (
                    <li key={idx}>{err}</li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>
      </div>

      <div className="card">
        <h3>Cloud Sync</h3>
        {syncStatus ? (
          <div className="sync">
            <p>Status: {syncStatus.lastStatus || 'unknown'}</p>
            <p>Last Sync: {syncStatus.lastSyncAt || 'never'}</p>
            <p>Conflicts: {syncStatus.lastConflictCount ?? 0}</p>
          </div>
        ) : (
          <p className="muted">Sync status unavailable.</p>
        )}
        {syncStatus && (syncStatus.lastConflictCount || 0) > 0 && (
          <div className="banner warn">Conflicts detected. Latest changes kept (LWW).</div>
        )}
        <div className="inline">
          <button onClick={() => toggleSync(true)}>Enable</button>
          <button onClick={() => toggleSync(false)}>Disable</button>
          <button onClick={runSync}>Run Sync</button>
        </div>
      </div>
    </div>
  )
}
