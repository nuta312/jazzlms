import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, auth } from '../api/client'

export default function Register() {
  const nav = useNavigate()
  const [f, setF] = useState({ firstName: '', lastName: '', email: '', username: '', password: '' })
  const [error, setError] = useState(null)
  const [settings, setSettings] = useState(null)
  const [accept, setAccept] = useState(false)
  useEffect(() => { api.get('/api/auth/settings').then(setSettings).catch(() => {}) }, [])
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    try {
      auth.save(await api.post('/api/auth/register', { ...f, acceptTerms: accept }))
      nav('/')
    } catch (err) { setError(err.message) }
  }

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={submit}>
        <div className="logo" style={{ marginBottom: 24 }}><span className="mark">J</span> Sign up</div>
        {error && <div className="error">{error}</div>}
        <label>First name</label><input value={f.firstName} onChange={set('firstName')} required />
        <label>Last name</label><input value={f.lastName} onChange={set('lastName')} required />
        <label>Email</label><input type="email" value={f.email} onChange={set('email')} required />
        <label>Username</label><input value={f.username} onChange={set('username')} required minLength={3} />
        <label>Password</label><input type="password" value={f.password} onChange={set('password')} required minLength={6} />
        {settings?.signupAllowed === false && <div className="error">Self-registration is disabled. Ask an administrator to create your account.</div>}
        {settings?.termsOfService && (
          <div className="tos"><div className="tos-text">{settings.termsOfService}</div>
            <label className="check"><input type="checkbox" checked={accept} onChange={(e) => setAccept(e.target.checked)} /> I accept the Terms of Service</label></div>
        )}
        <button className="btn btn-primary" disabled={settings?.signupAllowed === false}>Create account</button>
        <p className="hint" style={{ marginTop: 16 }}><Link to="/login">Back to login</Link></p>
      </form>
    </div>
  )
}
