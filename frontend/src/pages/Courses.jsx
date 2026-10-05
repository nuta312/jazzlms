import { useState } from 'react'
import { Link } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { fmtDate, useApi } from '../components/useApi'
import { api, auth, mode } from '../api/client'

export default function Courses() {
  const [q, setQ] = useState('')
  const { data, error, reload } = useApi(`/api/courses?search=${encodeURIComponent(q)}`)
  const isStaff = ['SUPER_ADMIN', 'ADMIN', 'TRAINER'].includes(auth.user()?.userType) && mode.canManage()

  const remove = async (c) => {
    if (!confirm(`Delete course "${c.name}"?\nYou can undo this from Reports → Timeline.`)) return
    try { await api.delete(`/api/courses/${c.id}`); reload() } catch (e) { alert(e.message) }
  }

  return (
    <Page title="Courses">
      <div className="toolbar">
        {isStaff && <Link to="/courses/new" className="btn btn-primary dropdown-caret">Add course</Link>}
        <div className="right"><input className="search" placeholder="Search" value={q} onChange={(e) => setQ(e.target.value)} /></div>
      </div>
      {error && <div className="error">{error}</div>}
      <table className="grid">
        <thead><tr><th>Course</th><th>Category</th><th>Learners</th><th>Last updated on</th><th>Options</th></tr></thead>
        <tbody>
          {(data || []).map((c) => (
            <tr key={c.id}>
              <td>
                <Link to={`/courses/${c.id}`}>{c.name}</Link> {c.code && <small className="hint">({c.code})</small>}
                {!c.active && <span className="badge">inactive</span>}
                {c.hiddenFromCatalog && <span className="badge gray">hidden</span>}
              </td>
              <td>{c.categoryName || '-'}</td>
              <td>{c.learnersCount}</td>
              <td>{fmtDate(c.updatedAt)}</td>
              <td>
                <Link to={`/courses/${c.id}`}>Open</Link>
                {isStaff && <>{' · '}<Link to={`/courses/${c.id}/edit`}>✎ Edit</Link>{' · '}
                  <button className="link-btn danger" onClick={() => remove(c)}>✕ Delete</button></>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <span className="count">1 to {data?.length ?? 0} of {data?.length ?? 0}</span>
    </Page>
  )
}
