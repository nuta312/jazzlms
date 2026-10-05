import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { fmtDate, useApi } from '../components/useApi'
import { api } from '../api/client'

const TIME_ZONES = ['UTC', 'Europe/London', 'Europe/Berlin', 'Europe/Moscow', 'Asia/Almaty', 'Asia/Bishkek', 'Asia/Tashkent', 'Asia/Novosibirsk', 'America/New_York']
const LANGS = [['en', 'English'], ['ru', 'Русский (Russian)'], ['kk', 'Қазақша (Kazakh)'], ['es', 'Español (Spanish)'], ['de', 'Deutsch (German)']]
const EMPTY = { name: '', title: '', description: '', language: 'en', timeZone: 'Asia/Almaty', defaultUserType: 'LEARNER', signupMode: 'MANUAL', announcement: '', active: true }

/** Add / Edit branch: разделы Identity, Locale, Announcement, Users — как в TalentLMS (без E-commerce). */
export default function BranchForm() {
  const { id } = useParams()
  const nav = useNavigate()
  const [f, setF] = useState(EMPTY)
  const [error, setError] = useState(null)
  const { data: members, reload } = useApi(id ? `/api/branches/${id}/users` : null)
  const { data: users } = useApi(id ? '/api/users' : null)
  const [pick, setPick] = useState('')
  const set = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  useEffect(() => { if (id) api.get(`/api/branches/${id}`).then((b) => setF({ ...EMPTY, ...b, title: b.title || '', description: b.description || '', announcement: b.announcement || '' })).catch((e) => setError(e.message)) }, [id])

  const submit = async (e) => {
    e.preventDefault(); setError(null)
    try { if (id) await api.put(`/api/branches/${id}`, f); else await api.post('/api/branches', f); nav('/branches') } catch (err) { setError(err.message) }
  }
  const addMember = async (e) => { e.preventDefault(); try { await api.post(`/api/branches/${id}/users/${pick}`); setPick(''); reload() } catch (err) { alert(err.message) } }
  const removeMember = async (u) => { try { await api.delete(`/api/branches/${id}/users/${u.id}`); reload() } catch (err) { alert(err.message) } }
  const candidates = (users || []).filter((u) => !(members || []).some((m) => m.id === u.id))

  return (
    <Page crumbs={[{ to: '/branches', label: 'Branches' }]} title={id ? `Edit branch: ${f.name}` : 'Add branch'}>
      <form className="form wide-form" onSubmit={submit}>
        {error && <div className="error">{error}</div>}
        <div className="section-bar">Identity</div>
        <div className="form-row"><label>Name</label><input type="text" placeholder="e.g. marketing" value={f.name} onChange={set('name')} required maxLength={60} pattern="[a-z0-9-]+" title="lowercase letters, digits and dashes" /></div>
        <div className="form-row"><label>Title</label><input type="text" placeholder="Title used in search engines" value={f.title} onChange={set('title')} maxLength={120} /></div>
        <div className="form-row wide"><label>Description</label><textarea placeholder="Short description up to 255 characters" value={f.description} onChange={set('description')} maxLength={255} /></div>
        <div className="form-row"><label /><span className="check"><input type="checkbox" checked={f.active} onChange={set('active')} /> Active</span></div>

        <div className="section-bar">Locale</div>
        <div className="form-row"><label>Branch language</label><select value={f.language} onChange={set('language')}>{LANGS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select></div>
        <div className="form-row"><label>Branch time zone</label><select value={f.timeZone} onChange={set('timeZone')}>{TIME_ZONES.map((z) => <option key={z}>{z}</option>)}</select></div>

        <div className="section-bar">Announcement</div>
        <div className="form-row wide"><label>Announcement</label><textarea placeholder="Shown to branch members on their home page" value={f.announcement} onChange={set('announcement')} maxLength={2000} /></div>

        <div className="section-bar">Users</div>
        <div className="form-row"><label>Default user type</label>
          <select value={f.defaultUserType} onChange={set('defaultUserType')}>
            <option value="LEARNER">Learner-Type</option><option value="TRAINER">Instructor (Trainer-Type)</option><option value="ADMIN">Admin-Type</option>
          </select></div>
        <div className="form-row"><label>Signup</label>
          <select value={f.signupMode} onChange={set('signupMode')}>
            <option value="MANUAL">Manually (from Admin)</option><option value="DIRECT">Direct (self signup)</option>
          </select></div>

        <div className="form-actions">
          <button className="btn btn-primary">{id ? 'Update branch' : 'Add branch'}</button>
          <span>or <a href="#" onClick={(e) => { e.preventDefault(); nav('/branches') }}>cancel</a></span>
        </div>
      </form>

      {id && (
        <>
          <div className="section-bar" style={{ marginTop: 26 }}>Members ({members?.length ?? 0})</div>
          <form className="toolbar" onSubmit={addMember}>
            <select value={pick} onChange={(e) => setPick(e.target.value)} required style={{ padding: 8 }}>
              <option value="">Add user to branch…</option>
              {candidates.map((u) => <option key={u.id} value={u.id}>{u.firstName} {u.lastName} ({u.email})</option>)}
            </select>
            <button className="btn btn-primary btn-sm">+ Add</button>
          </form>
          <table className="grid">
            <thead><tr><th>User</th><th>Email</th><th>Registration</th><th>Options</th></tr></thead>
            <tbody>
              {(members || []).map((u) => <tr key={u.id}><td>{u.firstName} {u.lastName}</td><td>{u.email}</td><td>{fmtDate(u.createdAt)}</td><td><button className="link-btn danger" onClick={() => removeMember(u)}>—</button></td></tr>)}
              {members?.length === 0 && <tr><td colSpan={4} className="hint">No members yet</td></tr>}
            </tbody>
          </table>
        </>
      )}
    </Page>
  )
}
