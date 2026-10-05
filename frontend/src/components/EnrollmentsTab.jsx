import { useState } from 'react'
import { fmtDate, useApi } from './useApi'
import { api } from '../api/client'

/** Users & progress: записанные на курс. Запись идёт через course-service, который проверяет пользователя по gRPC. */
export default function EnrollmentsTab({ courseId, manage }) {
  const { data: enrollments, reload } = useApi(`/api/courses/${courseId}/enrollments`)
  const { data: users } = useApi(manage ? '/api/users' : null)
  const [userId, setUserId] = useState('')
  const [role, setRole] = useState('LEARNER')
  const [msg, setMsg] = useState(null)

  const enroll = async (e) => {
    e.preventDefault(); setMsg(null)
    try { await api.post(`/api/courses/${courseId}/enrollments`, { userId, role }); setUserId(''); reload() } catch (err) { setMsg(err.message) }
  }
  const unenroll = async (en) => {
    if (!confirm('Remove from course?')) return
    try { await api.delete(`/api/enrollments/${en.id}`); reload() } catch (err) { alert(err.message) }
  }
  const candidates = manage && Array.isArray(users) ? users.filter((u) => !(enrollments || []).some((e) => e.userId === u.id)) : []

  return (
    <>
      {manage && (
        <form className="toolbar" onSubmit={enroll}>
          <select value={userId} onChange={(e) => setUserId(e.target.value)} required style={{ padding: 8 }}>
            <option value="">Select user…</option>
            {candidates.map((u) => <option key={u.id} value={u.id}>{u.firstName} {u.lastName} ({u.email})</option>)}
          </select>
          <select value={role} onChange={(e) => setRole(e.target.value)} style={{ padding: 8 }}>
            <option value="LEARNER">as Learner</option><option value="INSTRUCTOR">as Instructor</option>
          </select>
          <button className="btn btn-primary btn-sm">Enroll</button>
        </form>
      )}
      {msg && <div className="error" style={{ marginTop: 10 }}>{msg}</div>}
      <table className="grid">
        <thead><tr><th>User</th><th>Role</th><th>Progress</th><th>Enrolled</th><th>Completed</th>{manage && <th>Options</th>}</tr></thead>
        <tbody>
          {(enrollments || []).map((en) => (
            <tr key={en.id}>
              <td>{en.userFullName}<br /><small className="hint">{en.userEmail}</small></td>
              <td><span className={`role-pill sm ${en.role.toLowerCase()}`}>{en.role}</span></td>
              <td>{en.role === 'LEARNER' ? <><div className="progress"><div style={{ width: `${en.progress}%` }} /></div> {en.progress}%</> : '-'}</td>
              <td>{fmtDate(en.enrolledAt)}</td>
              <td>{en.completedAt ? <span className="badge ok">done</span> : '-'}</td>
              {manage && <td><button className="link-btn danger" onClick={() => unenroll(en)}>✕</button></td>}
            </tr>
          ))}
        </tbody>
      </table>
    </>
  )
}
