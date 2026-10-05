import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { useApi } from '../components/useApi'
import { api } from '../api/client'

const EMPTY = { name: '', eventType: '', recipient: '', active: true, delayMinutes: 0,
  subjectTemplate: '', bodyTemplate: 'Hi {{firstName}},\n\n' }

export default function NotificationForm() {
  const { id } = useParams()
  const nav = useNavigate()
  const { data: events } = useApi('/api/notifications/events')
  const { data: recipients } = useApi('/api/notifications/recipients')
  const [f, setF] = useState(EMPTY)
  const [error, setError] = useState(null)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  useEffect(() => { if (id) api.get(`/api/notifications/${id}`).then((r) => setF({ ...EMPTY, ...r })) }, [id])

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    try {
      const body = { ...f, delayMinutes: Number(f.delayMinutes) }
      if (id) await api.put(`/api/notifications/${id}`, body); else await api.post('/api/notifications', body)
      nav('/notifications')
    } catch (err) { setError(err.message) }
  }

  return (
    <Page crumbs={[{ to: '/notifications', label: 'Notifications' }]} title={id ? 'Edit notification' : 'Add notification'}>
      <form className="form" onSubmit={submit}>
        {error && <div className="error">{error}</div>}
        <div className="form-row"><label>Name</label><input type="text" value={f.name} onChange={set('name')} required maxLength={100} /></div>
        <div className="form-row"><label>Event</label>
          <select value={f.eventType} onChange={set('eventType')} required>
            <option value="">Select event</option>
            {(events || []).map((e) => <option key={e} value={e}>{e.replaceAll('_', ' ').toLowerCase()}</option>)}
          </select></div>
        <div className="form-row"><label>Recipient</label>
          <select value={f.recipient} onChange={set('recipient')} required>
            <option value="">Select recipient</option>
            {(recipients || []).map((r) => <option key={r} value={r}>{r.replaceAll('_', ' ').toLowerCase()}</option>)}
          </select></div>
        <div className="form-row"><label>Delay (minutes)</label><input type="number" min="0" value={f.delayMinutes} onChange={set('delayMinutes')} /></div>
        <div className="form-row"><label>Subject</label><input type="text" value={f.subjectTemplate} onChange={set('subjectTemplate')} required /></div>
        <div className="form-row wide"><label>Message</label><textarea value={f.bodyTemplate} onChange={set('bodyTemplate')} required /></div>
        <div className="form-row"><label /><span className="hint">Placeholders: {'{{firstName}} {{lastName}} {{email}} {{username}} {{courseName}} {{portalName}}'}</span></div>
        <div className="form-row"><label /><span className="check"><input type="checkbox" checked={f.active} onChange={set('active')} /> Active</span></div>
        <div className="form-actions">
          <button className="btn btn-primary">{id ? 'Save' : 'Create notification'}</button>
          <span>or <a href="/notifications" onClick={(e) => { e.preventDefault(); nav('/notifications') }}>cancel</a></span>
        </div>
      </form>
    </Page>
  )
}
