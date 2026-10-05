import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { fmtDate, useApi } from '../components/useApi'
import { api } from '../api/client'

export default function GroupForm() {
  const { id } = useParams()
  const nav = useNavigate()
  const [f, setF] = useState({ name: '', description: '', active: true })
  const [error, setError] = useState(null)
  const { data: members, reload } = useApi(id ? `/api/groups/${id}/users` : null)
  const { data: users } = useApi(id ? '/api/users' : null)
  const [pick, setPick] = useState('')
  const set = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  useEffect(() => { if (id) api.get(`/api/groups/${id}`).then((g) => setF({ name: g.name, description: g.description || '', active: g.active })).catch((e) => setError(e.message)) }, [id])

  const submit = async (e) => {
    e.preventDefault(); setError(null)
    try { if (id) await api.put(`/api/groups/${id}`, f); else await api.post('/api/groups', f); nav('/groups') } catch (err) { setError(err.message) }
  }
  const addMember = async (e) => { e.preventDefault(); try { await api.post(`/api/groups/${id}/users/${pick}`); setPick(''); reload() } catch (err) { alert(err.message) } }
  const removeMember = async (u) => { try { await api.delete(`/api/groups/${id}/users/${u.id}`); reload() } catch (err) { alert(err.message) } }
  const candidates = (users || []).filter((u) => !(members || []).some((m) => m.id === u.id))

  return (
    <Page crumbs={[{ to: '/groups', label: 'Groups' }]} title={id ? `Edit group: ${f.name}` : 'Add group'}>
      <form className="form" onSubmit={submit}>
        {error && <div className="error">{error}</div>}
        <div className="form-row"><label>Name</label><input type="text" placeholder="e.g. QA Spring 2026" value={f.name} onChange={set('name')} required maxLength={80} /></div>
        <div className="form-row wide"><label>Description</label><textarea placeholder="Short description up to 500 characters" value={f.description} onChange={set('description')} maxLength={500} /></div>
        <div className="form-row"><label /><span className="check"><input type="checkbox" checked={f.active} onChange={set('active')} /> Active</span></div>
        <div className="form-actions">
          <button className="btn btn-primary">{id ? 'Update group' : 'Add group'}</button>
          <span>or <a href="#" onClick={(e) => { e.preventDefault(); nav('/groups') }}>cancel</a></span>
        </div>
      </form>
      {id && (
        <>
          <div className="section-bar" style={{ marginTop: 26 }}>Members ({members?.length ?? 0})</div>
          <form className="toolbar" onSubmit={addMember}>
            <select value={pick} onChange={(e) => setPick(e.target.value)} required style={{ padding: 8 }}>
              <option value="">Add user to group…</option>
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
