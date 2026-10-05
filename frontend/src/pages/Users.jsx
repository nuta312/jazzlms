import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { fmtDate, useApi } from '../components/useApi'
import { api, auth, download, impersonation } from '../api/client'

export const TYPE_LABEL = { SUPER_ADMIN: 'SuperAdmin', ADMIN: 'Admin-Type', TRAINER: 'Instructor', LEARNER: 'Learner-Type' }

/** "43 minutes ago" для недавних входов, дата — для старых (как колонка LAST LOGIN в TalentLMS). */
export function lastLogin(iso) {
  if (!iso) return '-'
  const min = Math.floor((Date.now() - new Date(iso).getTime()) / 60000)
  if (min < 1) return 'A few moments ago'
  if (min < 60) return `${min} minute${min > 1 ? 's' : ''} ago`
  if (min < 24 * 60) return `${Math.floor(min / 60)} hour${min >= 120 ? 's' : ''} ago`
  if (min < 48 * 60) return 'Yesterday'
  return fmtDate(iso)
}

export default function Users() {
  const nav = useNavigate()
  const me = auth.user()
  // преподаватель видит список (чтобы записывать на курс), но управлять пользователями может только админ
  const isAdmin = ['SUPER_ADMIN', 'ADMIN'].includes(me?.userType)
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('')          // '' | 'true' | 'false'
  const [view, setView] = useState(localStorage.getItem('jazzlms.usersView') || 'table')
  const [menu, setMenu] = useState(null)            // 'add' | 'filter'
  const popRef = useRef(null)

  const qs = new URLSearchParams({ ...(q && { search: q }), ...(status && { active: status }) }).toString()
  const { data, error, reload } = useApi(`/api/users?${qs}`)

  useEffect(() => {
    const close = (e) => { if (popRef.current && !popRef.current.contains(e.target)) setMenu(null) }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  const remove = async (u) => {
    if (!confirm(`Delete ${u.firstName} ${u.lastName}?`)) return
    try { await api.delete(`/api/users/${u.id}`); reload() } catch (e) { alert(e.message) }
  }
  const loginAs = async (u) => {
    if (!confirm(`Log into the account of ${u.firstName} ${u.lastName}?\nYou can return to your own account from the yellow bar.`)) return
    try { impersonation.start(await api.post(`/api/auth/impersonate/${u.id}`)); nav('/'); nav(0) } catch (e) { alert(e.message) }
  }
  const changeView = (v) => { setView(v); localStorage.setItem('jazzlms.usersView', v) }

  const Actions = ({ u }) => (
    <span className="row-actions">
      <Link to={`/users/${u.id}/report`} data-tip="Reports"><svg width="18" height="16" viewBox="0 0 18 16" fill="currentColor"><rect x="0" y="11" width="3" height="5"/><rect x="5" y="7" width="3" height="9"/><rect x="10" y="3" width="3" height="13"/><rect x="15" y="0" width="3" height="16"/></svg></Link>
      {isAdmin && u.id !== me.id && u.userType !== 'SUPER_ADMIN' && <button onClick={() => loginAs(u)} data-tip="Log into account">⇥</button>}
      {isAdmin && <Link to={`/users/${u.id}/edit`} data-tip="Edit">✎</Link>}
      {isAdmin && u.userType !== 'SUPER_ADMIN' && <button onClick={() => remove(u)} data-tip="Delete">✕</button>}
    </span>
  )

  return (
    <Page title="Users">
      <div className="toolbar" ref={menu === 'add' ? popRef : null}>
        {isAdmin && (
          <div className="dropdown split">
            <Link to="/users/new" className="btn btn-primary">Add user</Link>
            <button className="btn btn-primary caret" onClick={() => setMenu(menu === 'add' ? null : 'add')}>▾</button>
            {menu === 'add' && <div className="dropdown-menu"><Link to="/users/import">Import user(s)</Link></div>}
          </div>
        )}
        <div className="right">
          <div className="seg">
            <button className={view === 'table' ? 'active' : ''} title="Table view" onClick={() => changeView('table')}>☰</button>
            <button className={view === 'grid' ? 'active' : ''} title="Grid view" onClick={() => changeView('grid')}>▦</button>
          </div>
        </div>
      </div>
      {error && <div className="error">{error}</div>}

      {view === 'table' ? (
        <table className="grid hover-actions">
          <thead><tr><th>User</th><th>Email</th><th>User type</th><th>Registration</th><th>Last login</th><th>Options</th></tr></thead>
          <tbody>
            {(data || []).map((u) => (
              <tr key={u.id}>
                <td>{u.firstName[0]}. {u.lastName}{!u.active && <span className="badge">inactive</span>}</td>
                <td>{u.email}</td>
                <td>{TYPE_LABEL[u.userType]}</td>
                <td>{fmtDate(u.createdAt)}</td>
                <td>{lastLogin(u.lastLoginAt)}</td>
                <td className="options"><span className="dots">•••</span><Actions u={u} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <div className="course-cards">
          {(data || []).map((u) => (
            <div className="course-card user-card" key={u.id}>
              <div className="avatar">{u.firstName[0]}{u.lastName[0]}</div>
              <b>{u.firstName} {u.lastName}{!u.active && <span className="badge">inactive</span>}</b>
              <small className="hint">{u.email}</small>
              <small>{TYPE_LABEL[u.userType]} · last login: {lastLogin(u.lastLoginAt)}</small>
              <Actions u={u} />
            </div>
          ))}
        </div>
      )}

      <div className="toolbar" style={{ marginTop: 14 }}>
        <span className="count" style={{ marginTop: 0 }}>{data?.length ? 1 : 0} to {data?.length ?? 0} of {data?.length ?? 0}</span>
        <div className="right" ref={menu === 'filter' ? popRef : null}>
          <button className="icon-btn" data-tip="Save as CSV" onClick={() => download(`/api/users/export?${qs}`, 'users.csv')}>
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2"><path d="M10 2v10m-4-4 4 4 4-4M3 14v4h14v-4" /></svg>
          </button>
          <div className="dropdown">
            <button className={`icon-btn ${status ? 'on' : ''}`} data-tip="Filter" onClick={() => setMenu(menu === 'filter' ? null : 'filter')}>
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2"><path d="M2 3h16l-6 8v6l-4-2v-4z" /></svg>
            </button>
            {menu === 'filter' && (
              <div className="dropdown-menu up">
                <div className="menu-title">Status</div>
                {[['true', 'Active'], ['false', 'Inactive']].map(([v, l]) => (
                  <a key={v} href="#" className={status === v ? 'selected' : ''}
                    onClick={(e) => { e.preventDefault(); setStatus(status === v ? '' : v); setMenu(null) }}>{status === v ? '✓ ' : ''}{l}</a>
                ))}
              </div>
            )}
          </div>
          <input className="search" placeholder="Search" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
      </div>
    </Page>
  )
}
