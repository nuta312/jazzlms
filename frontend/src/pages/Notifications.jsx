import { useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { fmtDateTime, useApi } from '../components/useApi'
import { api } from '../api/client'

const EVENT_LABEL = {
  USER_CREATED: 'User registration (by admin)', USER_SIGNED_UP: 'User self registration', USER_LOGGED_IN: 'User login',
  COURSE_CREATED: 'Course created', COURSE_UPDATED: 'Course updated', USER_ENROLLED: 'Added user to course',
  COURSE_COMPLETED: 'User completed course', ASSIGNMENT_SUBMITTED: 'Assignment submission',
  COURSE_DELETED: 'Course deletion', COURSE_RESTORED: 'Undelete course', USER_DELETED: 'User deletion', USER_IMPERSONATED: 'Log into account',
  UNIT_ADDED: 'Unit added to course', UNIT_COMPLETED: 'User completed unit',
}
const RECIPIENT_LABEL = { RELATED_USER: 'Related user', ACCOUNT_OWNER: 'Account owner', COURSE_INSTRUCTORS: 'Course instructors' }

/** Events Engine: три вкладки, как в TalentLMS. Данные — из MongoDB notification-service. */
export default function Notifications({ tab }) {
  const path = { rules: '/api/notifications', history: '/api/notifications/history', pending: '/api/notifications/pending' }[tab]
  const { data, error, reload } = useApi(path)
  const [open, setOpen] = useState(null)

  const removeRule = async (r) => {
    if (!confirm(`Delete notification "${r.name}"?`)) return
    await api.delete(`/api/notifications/${r.id}`); reload()
  }
  const clearHistory = async () => {
    if (!confirm('Clear notification history?')) return
    await api.delete('/api/notifications/history'); reload()
  }
  const removeMsg = async (m) => { await api.delete(`/api/notifications/messages/${m.id}`); reload() }

  return (
    <Page title="Notifications">
      <div className="tabs">
        <NavLink to="/notifications" end>Notifications</NavLink>
        <NavLink to="/notifications/history">History</NavLink>
        <NavLink to="/notifications/pending">Pending notifications</NavLink>
      </div>
      {error && <div className="error">{error}</div>}

      {tab === 'rules' && (
        <>
          <Link to="/notifications/new" className="btn btn-primary dropdown-caret">Add notification</Link>
          <table className="grid">
            <thead><tr><th>Name</th><th>Event</th><th>Recipient</th><th>Delay</th><th>Options</th></tr></thead>
            <tbody>
              {(data || []).map((r) => (
                <tr key={r.id}>
                  <td>{r.name}{!r.active && <span className="badge">inactive</span>}</td>
                  <td>{EVENT_LABEL[r.eventType] || r.eventType}</td>
                  <td>{RECIPIENT_LABEL[r.recipient]}</td>
                  <td>{r.delayMinutes ? `${r.delayMinutes} min` : 'immediately'}</td>
                  <td><Link to={`/notifications/${r.id}/edit`}>✎ Edit</Link>{' · '}<button className="link-btn danger" onClick={() => removeRule(r)}>✕ Delete</button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <span className="count">1 to {data?.length ?? 0} of {data?.length ?? 0}</span>
        </>
      )}

      {tab !== 'rules' && (
        <>
          {tab === 'history' && <button className="btn btn-danger" onClick={clearHistory}>Clear notification history</button>}
          <table className="grid">
            <thead><tr><th>Recipient</th><th>Subject</th><th>{tab === 'history' ? 'Date' : 'Scheduled for'}</th><th>Options</th></tr></thead>
            <tbody>
              {(data || []).map((m) => (
                <tr key={m.id}>
                  <td>{m.recipientEmail}</td>
                  <td>
                    <button className="link-btn" onClick={() => setOpen(open === m.id ? null : m.id)}>{m.subject}</button>
                    {open === m.id && <pre className="email">{m.body}</pre>}
                  </td>
                  <td>{fmtDateTime(tab === 'history' ? m.sentAt : m.scheduledAt)}</td>
                  <td><button className="link-btn danger" onClick={() => removeMsg(m)}>✕</button></td>
                </tr>
              ))}
              {data?.length === 0 && <tr><td colSpan={4} className="hint">-</td></tr>}
            </tbody>
          </table>
          {tab === 'history' && <span className="count">1 to {data?.length ?? 0} of {data?.length ?? 0}</span>}
        </>
      )}
    </Page>
  )
}
