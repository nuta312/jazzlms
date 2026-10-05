import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { api } from '../api/client'

const EMPTY = { firstName: '', lastName: '', email: '', username: '', password: '', bio: '',
  userType: 'LEARNER', timeZone: 'Asia/Almaty', language: 'en', active: true, excludeFromEmails: false }

const TIME_ZONES = ['UTC', 'Europe/London', 'Europe/Berlin', 'Europe/Moscow', 'Asia/Almaty', 'Asia/Bishkek', 'Asia/Tashkent', 'America/New_York']
const LANGS = [['en', 'English'], ['ru', 'Русский (Russian)'], ['kk', 'Қазақша (Kazakh)'], ['es', 'Español (Spanish)'], ['de', 'Deutsch (German)']]

/** Одна форма на создание и редактирование (аналог Add user / Edit user). */
export default function UserForm({ embedded = false }) {
  const { id } = useParams()
  const nav = useNavigate()
  const [f, setF] = useState(EMPTY)
  const [error, setError] = useState(null)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  useEffect(() => {
    if (id) api.get(`/api/users/${id}`).then((u) => setF({ ...EMPTY, ...u, password: '' })).catch((e) => setError(e.message))
  }, [id])

  const submit = async (e, addAnother = false) => {
    e.preventDefault()
    setError(null)
    const body = { ...f, password: f.password || null }
    try {
      if (id) await api.put(`/api/users/${id}`, body)
      else await api.post('/api/users', body)
      if (addAnother) setF(EMPTY); else nav('/users')
    } catch (err) { setError(err.message) }
  }

  const body = (
      <form className="form" onSubmit={submit}>
        {error && <div className="error">{error}</div>}
        <div className="form-row"><label>First name</label><input type="text" placeholder="e.g. John" value={f.firstName} onChange={set('firstName')} required maxLength={50} /></div>
        <div className="form-row"><label>Last name</label><input type="text" placeholder="e.g. Doe" value={f.lastName} onChange={set('lastName')} required /></div>
        <div className="form-row"><label>Email address</label><input type="email" placeholder="e.g. jdoe@example.com" value={f.email} onChange={set('email')} required /></div>
        <div className="divider" />
        <div className="form-row"><label>Username</label><input type="text" placeholder="e.g. jdoe" value={f.username} onChange={set('username')} required disabled={!!id} /></div>
        <div className="form-row"><label>Password</label><input type="password" placeholder={id ? 'Leave blank to keep current' : 'Blank for random password'} value={f.password} onChange={set('password')} /></div>
        <div className="divider" />
        <div className="form-row wide"><label>Bio</label><textarea placeholder="Short description up to 800 characters" value={f.bio || ''} onChange={set('bio')} maxLength={800} /></div>
        <div className="form-row"><label>User type</label>
          <select value={f.userType} onChange={set('userType')}>
            <option value="SUPER_ADMIN">SuperAdmin</option>
            <option value="ADMIN">Admin-Type</option>
            <option value="TRAINER">Instructor (Trainer-Type)</option>
            <option value="LEARNER">Learner-Type</option>
          </select></div>
        <div className="form-row"><label>Time zone</label>
          <select value={f.timeZone} onChange={set('timeZone')}>{TIME_ZONES.map((z) => <option key={z}>{z}</option>)}</select></div>
        <div className="form-row"><label>Language</label>
          <select value={f.language} onChange={set('language')}>{LANGS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select></div>
        <div className="form-row"><label /><span className="check"><input type="checkbox" checked={f.active} onChange={set('active')} /> Active</span></div>
        <div className="form-row"><label /><span className="check"><input type="checkbox" checked={f.excludeFromEmails} onChange={set('excludeFromEmails')} /> Exclude from emails</span></div>
        <div className="form-actions">
          <button className="btn btn-primary">{id ? 'Save' : 'Add user'}</button>
          {!id && <button type="button" className="btn btn-light" onClick={(e) => submit(e, true)}>Add user and add another</button>}
          <span>or <a href="/users" onClick={(e) => { e.preventDefault(); nav('/users') }}>cancel</a></span>
        </div>
      </form>
  )
  if (embedded) return body
  return <Page crumbs={[{ to: '/users', label: 'Users' }]} title={id ? 'Edit user' : 'Add user'}>{body}</Page>
}
