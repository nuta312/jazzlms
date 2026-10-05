import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, auth } from '../api/client'

/** Принудительная (или добровольная) смена пароля. Сюда ведёт Login, если сервер вернул mustChangePassword. */
export default function ChangePassword() {
  const nav = useNavigate()
  const forced = new URLSearchParams(window.location.search).get('forced') === '1'
  const [f, setF] = useState({ currentPassword: '', newPassword: '', repeat: '' })
  const [error, setError] = useState(null)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })
  const submit = async (e) => {
    e.preventDefault(); setError(null)
    if (f.newPassword !== f.repeat) return setError('Passwords do not match')
    try { await api.post('/api/auth/password', { currentPassword: f.currentPassword, newPassword: f.newPassword }); nav('/') }
    catch (err) { setError(err.message) }
  }
  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={submit}>
        <div className="logo" style={{ marginBottom: 12 }}><span className="mark">J</span> Change password</div>
        {forced && <p className="hint" style={{ marginBottom: 14 }}>Your administrator requires a new password before you continue.</p>}
        {error && <div className="error">{error}</div>}
        <label>Current password</label><input type="password" value={f.currentPassword} onChange={set('currentPassword')} required autoFocus />
        <label>New password</label><input type="password" value={f.newPassword} onChange={set('newPassword')} required minLength={6} />
        <label>Repeat new password</label><input type="password" value={f.repeat} onChange={set('repeat')} required minLength={6} />
        <button className="btn btn-primary">Change password</button>
        {!forced && <p className="hint" style={{ marginTop: 16 }}><a href="/" onClick={(e) => { e.preventDefault(); nav(-1) }}>Back</a></p>}
        <p className="hint" style={{ marginTop: 16 }}><button type="button" className="link-btn" onClick={() => { auth.clear(); nav('/login') }}>Log out</button></p>
      </form>
    </div>
  )
}
