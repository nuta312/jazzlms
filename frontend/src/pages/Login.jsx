import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, auth } from '../api/client'

export default function Login() {
  const nav = useNavigate()
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('admin123')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const [settings, setSettings] = useState(null)
  useEffect(() => { api.get('/api/auth/settings').then(setSettings).catch(() => {}) }, [])   // публичный: Sign up разрешён? как называется портал?

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setError(null)
    try {
      const res = await api.post('/api/auth/login', { username, password })
      auth.save(res)
      nav(res.mustChangePassword ? '/change-password?forced=1' : '/')   // политика паролей из Account & Settings
    } catch (err) {
      setError(err.message)
    } finally { setBusy(false) }
  }

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={submit}>
        <div className="logo" style={{ marginBottom: 24 }}><span className="mark">J</span> {settings?.siteName?.toLowerCase() || 'jazzlms'}</div>
        {error && <div className="error">{error}</div>}
        <label>Username or email</label>
        <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
        <label>Password</label>
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        <button className="btn btn-primary" disabled={busy}>{busy ? 'Signing in…' : 'Login'}</button>
        {settings?.signupAllowed !== false && <p className="hint" style={{ marginTop: 16 }}>No account? <Link to="/register">Sign up</Link></p>}
        <p className="hint">Default admin: admin / admin123</p>
      </form>
    </div>
  )
}
