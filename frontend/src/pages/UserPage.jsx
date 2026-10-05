import { useState } from 'react'
import { Link, NavLink, useParams } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { fmtDate, fmtDateTime, useApi } from '../components/useApi'
import { api } from '../api/client'
import UserForm from './UserForm.jsx'

/** Переключатель Profile / Progress / Infographic справа, как в TalentLMS. */
export function UserViewToggle({ id, active }) {
  return (
    <div className="view-toggle-tabs">
      <Link to={`/users/${id}/edit`} className={active === 'profile' ? 'active' : ''}>Profile</Link>
      <Link to={`/users/${id}/report`} className={active === 'progress' ? 'active' : ''}>Progress</Link>
      <Link to={`/users/${id}/infographic`} className={active === 'infographic' ? 'active' : ''}>Infographic</Link>
    </div>
  )
}

/**
 * Карточка пользователя: Info (форма), Courses (записи), Groups, Branches, Files.
 * Данные приходят из трёх сервисов: user-service (профиль, группы, ветки), course-service (курсы, файлы).
 */
export default function UserPage({ tab = 'info' }) {
  const { id } = useParams()
  const { data: user } = useApi(`/api/users/${id}`)
  const name = user ? `${user.firstName} ${user.lastName}` : '…'
  return (
    <Page crumbs={[{ to: '/users', label: 'Users' }]} title={name}>
      <div className="tabs with-toggle">
        <NavLink to={`/users/${id}/edit`} end>Info</NavLink>
        <NavLink to={`/users/${id}/courses`}>Courses</NavLink>
        <NavLink to={`/users/${id}/groups`}>Groups</NavLink>
        <NavLink to={`/users/${id}/branches`}>Branches</NavLink>
        <NavLink to={`/users/${id}/files`}>Files</NavLink>
        <UserViewToggle id={id} active="profile" />
      </div>
      {tab === 'info' && <UserForm embedded />}
      {tab === 'courses' && <CoursesTab id={id} />}
      {tab === 'groups' && <MembershipTab id={id} kind="groups" label="group" />}
      {tab === 'branches' && <MembershipTab id={id} kind="branches" label="branch" />}
      {tab === 'files' && <FilesTab id={id} />}
    </Page>
  )
}

function CoursesTab({ id }) {
  const { data, error, reload } = useApi(`/api/enrollments/user/${id}`)
  const [q, setQ] = useState('')
  const rows = (data || []).filter((e) => e.course.name.toLowerCase().includes(q.toLowerCase()))
  const unenroll = async (e) => {
    if (!confirm(`Remove from "${e.course.name}"?`)) return
    try { await api.delete(`/api/enrollments/${e.enrollmentId}`); reload() } catch (err) { alert(err.message) }
  }
  return (
    <>
      {error && <div className="error">{error}</div>}
      <table className="grid">
        <thead><tr><th>Course</th><th>Role</th><th>Enrolled on</th><th>Completion date</th><th>Options</th></tr></thead>
        <tbody>
          {rows.map((e) => (
            <tr key={e.enrollmentId}>
              <td><Link to={`/courses/${e.course.id}`}>{e.course.name}</Link></td>
              <td><span className={`role-pill ${e.role.toLowerCase()}`}>{e.role}</span></td>
              <td>{fmtDate(e.enrolledAt)}</td>
              <td>{e.completedAt ? fmtDate(e.completedAt) : '-'}</td>
              <td><button className="link-btn danger" title="Remove from course" onClick={() => unenroll(e)}>—</button></td>
            </tr>
          ))}
          {data?.length === 0 && <tr><td colSpan={5} className="hint">Not enrolled in any course</td></tr>}
        </tbody>
      </table>
      <div className="toolbar" style={{ marginTop: 14 }}>
        <span className="count" style={{ marginTop: 0 }}>{rows.length ? 1 : 0} to {rows.length} of {data?.length ?? 0}</span>
        <div className="right"><input className="search" placeholder="Search" value={q} onChange={(e) => setQ(e.target.value)} /></div>
      </div>
    </>
  )
}

/** Одна вкладка на группы и ветки: список членства + добавление из выпадающего списка. */
function MembershipTab({ id, kind, label }) {
  const { data: mine, error, reload } = useApi(`/api/users/${id}/${kind}`)
  const { data: all } = useApi(`/api/${kind}`)
  const [pick, setPick] = useState('')
  const key = kind === 'groups' ? 'group' : 'branch'
  const candidates = (all || []).map((x) => x[key]).filter((x) => !(mine || []).some((m) => m.id === x.id))
  const add = async (e) => { e.preventDefault(); try { await api.post(`/api/${kind}/${pick}/users/${id}`); setPick(''); reload() } catch (err) { alert(err.message) } }
  const remove = async (x) => { try { await api.delete(`/api/${kind}/${x.id}/users/${id}`); reload() } catch (err) { alert(err.message) } }
  return (
    <>
      {error && <div className="error">{error}</div>}
      <form className="toolbar" onSubmit={add}>
        <select value={pick} onChange={(e) => setPick(e.target.value)} required style={{ padding: 8 }}>
          <option value="">Add to {label}…</option>
          {candidates.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}
        </select>
        <button className="btn btn-primary btn-sm">+ Add</button>
      </form>
      {mine?.length === 0 ? (
        <div className="empty-state"><div className="empty-art">👥</div><p>{kind === 'groups' ? 'You do not belong in any group' : 'Not a member of any branch'}</p></div>
      ) : (
        <table className="grid">
          <thead><tr><th>{label[0].toUpperCase() + label.slice(1)}</th><th>Description</th><th>Options</th></tr></thead>
          <tbody>
            {(mine || []).map((x) => (
              <tr key={x.id}>
                <td><Link to={`/${kind}/${x.id}/edit`}>{x.name}</Link>{!x.active && <span className="badge">inactive</span>}</td>
                <td>{x.description || '-'}</td>
                <td><button className="link-btn danger" title={`Remove from ${label}`} onClick={() => remove(x)}>—</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}

function FilesTab({ id }) {
  const { data, error } = useApi(`/api/units/files/${id}`)
  const mb = (b) => `${(b / 1024 / 1024).toFixed(1)} MB`
  return (
    <>
      {error && <div className="error">{error}</div>}
      <div className="dropzone slim"><span>☁</span> Files this user uploaded to course units (video, presentations)</div>
      <table className="grid">
        <thead><tr><th>Name</th><th>Creation place</th><th>Type</th><th>Size</th><th>Uploaded</th></tr></thead>
        <tbody>
          {(data || []).map((f) => (
            <tr key={f.unitId}>
              <td><Link to={`/units/${f.unitId}`}>{f.fileName}</Link></td>
              <td>Unit «{f.unitName}»</td>
              <td>{f.contentType}</td>
              <td>{mb(f.size)}</td>
              <td>{fmtDateTime(f.uploadedAt)}</td>
            </tr>
          ))}
          {data?.length === 0 && <tr><td colSpan={5} className="hint">-</td></tr>}
        </tbody>
      </table>
    </>
  )
}
