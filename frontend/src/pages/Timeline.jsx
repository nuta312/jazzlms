import { useEffect, useMemo, useState } from 'react'
import { NavLink, useSearchParams } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { useApi } from '../components/useApi'
import { api, auth, download } from '../api/client'

/** Метка события: текст + цвет, как LOGIN / CREATE / ADD / DELETE в TalentLMS. */
export const LABEL = {
  USER_LOGGED_IN: ['Login', 'green'], USER_CREATED: ['Register', 'red'], USER_SIGNED_UP: ['Signup', 'red'],
  USER_DELETED: ['Delete', 'red'], USER_IMPERSONATED: ['Login as', 'orange'], COURSE_CREATED: ['Create', 'red'], COURSE_UPDATED: ['Update', 'gray'],
  COURSE_DELETED: ['Delete', 'red'], COURSE_RESTORED: ['Restore', 'green'], USER_ENROLLED: ['Add', 'orange'],
  COURSE_COMPLETED: ['Complete', 'blue'], UNIT_ADDED: ['Unit', 'orange'], UNIT_COMPLETED: ['Progress', 'blue'],
  ASSIGNMENT_SUBMITTED: ['Submit', 'gray'], CERTIFICATE_ISSUED: ['Certificate', 'green'], TEST_COMPLETED: ['Test', 'blue'],
}
export const EVENT_NAME = {
  USER_LOGGED_IN: 'User log in', USER_CREATED: 'User registration', USER_SIGNED_UP: 'User self registration',
  USER_DELETED: 'User deletion', USER_IMPERSONATED: 'Log into account', COURSE_CREATED: 'Course creation', COURSE_UPDATED: 'Course update',
  COURSE_DELETED: 'Course deletion', COURSE_RESTORED: 'Undelete course', USER_ENROLLED: 'Added user to course',
  COURSE_COMPLETED: 'User completed course', UNIT_ADDED: 'Unit added', UNIT_COMPLETED: 'Unit completion',
  ASSIGNMENT_SUBMITTED: 'Assignment submission', CERTIFICATE_ISSUED: 'Certificate issued', TEST_COMPLETED: 'Test completion',
}

const iso = (d) => d.toISOString().slice(0, 10)
const monthAgo = () => { const d = new Date(); d.setDate(d.getDate() - 30); return iso(d) }

export function ago(isoDate) {
  const s = Math.max(1, Math.round((Date.now() - new Date(isoDate).getTime()) / 1000))
  const units = [[86400 * 30, 'month'], [86400, 'day'], [3600, 'hour'], [60, 'minute'], [1, 'second']]
  for (const [size, name] of units) {
    if (s >= size) { const n = Math.floor(s / size); return `${n} ${name}${n > 1 ? 's' : ''} ago` }
  }
  return 'just now'
}

/** Подряд идущие логины одного пользователя схлопываем в одну строку "(22 times)". */
export function collapseLogins(items) {
  const rows = []
  ;(items || []).forEach((e) => {
    const last = rows[rows.length - 1]
    if (last && e.type === 'USER_LOGGED_IN' && last.type === e.type && last.userId === e.userId) last.times += 1
    else rows.push({ ...e, times: 1 })
  })
  return rows
}

export function ReportTabs() {
  return (
    <div className="tabs">
      <NavLink to="/reports" end>Overview</NavLink>
      <NavLink to="/timeline">Timeline</NavLink>
    </div>
  )
}

/**
 * Reports -> Timeline. Фильтры уходят query-параметрами в analytics-service,
 * который собирает из них запрос к MongoDB. Страница (page/size) — тоже на сервере.
 */
export default function Timeline() {
  const me = auth.user()
  const isAdmin = ['SUPER_ADMIN', 'ADMIN'].includes(me?.userType)
  const [search] = useSearchParams()
  const [f, setF] = useState({ from: monthAgo(), to: iso(new Date()), type: '', userId: search.get('userId') || '', courseId: '' })
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [deleted, setDeleted] = useState([])
  const { data: users } = useApi('/api/users')
  const { data: courses } = useApi('/api/courses')

  const qs = useMemo(() => {
    const p = new URLSearchParams()
    Object.entries(f).forEach(([k, v]) => v && p.set(k, v))
    return p.toString()
  }, [f])
  const { data, error, reload } = useApi(`/api/analytics/timeline?${qs}&page=${page}&size=${size}`)

  const loadDeleted = () => { if (isAdmin) api.get('/api/courses/deleted').then((d) => setDeleted(d.map((c) => c.id))).catch(() => {}) }
  useEffect(loadDeleted, []) // eslint-disable-line

  const change = (k, v) => { setF({ ...f, [k]: v }); setPage(0) }
  const reset = () => { setF({ from: '', to: '', type: '', userId: '', courseId: '' }); setPage(0) }
  const userName = (id) => { const u = (users || []).find((x) => x.id === id); return u ? `${u.firstName} ${u.lastName}` : null }

  const rows = collapseLogins(data?.items)

  const undo = async (e) => { try { await api.post(`/api/courses/${e.payload.courseId}/restore`); loadDeleted(); setTimeout(reload, 1200) } catch (err) { alert(err.message) } }
  const purge = async (e) => {
    if (!confirm(`Are you sure you want to delete the course "${e.payload.courseName}"?\nThis action cannot be undone`)) return
    try { await api.delete(`/api/courses/${e.payload.courseId}/permanent`); loadDeleted() } catch (err) { alert(err.message) }
  }

  const exportCsv = () => download(`/api/analytics/timeline/export?${qs}`, 'timeline.csv').catch((err) => alert(err.message))

  const total = data?.total ?? 0
  const pages = Math.max(1, Math.ceil(total / size))
  const shownButtons = new Set()

  return (
    <Page crumbs={[{ to: '/reports', label: 'Reports' }]} title="Timeline">
      <ReportTabs />
      <div className="filters">
        <label>From<input type="date" value={f.from} max={f.to || undefined} onChange={(e) => change('from', e.target.value)} /></label>
        <label>To<input type="date" value={f.to} min={f.from || undefined} max={iso(new Date())} onChange={(e) => change('to', e.target.value)} /></label>
        <label>Event
          <select value={f.type} onChange={(e) => change('type', e.target.value)}>
            <option value="" />{Object.entries(EVENT_NAME).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select></label>
        <label>User
          <select value={f.userId} onChange={(e) => change('userId', e.target.value)}>
            <option value="" />{(users || []).map((u) => <option key={u.id} value={u.id}>{u.firstName[0]}. {u.lastName}</option>)}
          </select></label>
        <label>Course
          <select value={f.courseId} onChange={(e) => change('courseId', e.target.value)}>
            <option value="" />{(courses || []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select></label>
        {Object.values(f).some(Boolean) && <button className="link-btn danger reset" title="Reset filter" onClick={reset}>✕</button>}
      </div>
      {error && <div className="error">{error}</div>}

      <table className="grid events">
        <thead><tr><th>Events</th></tr></thead>
        <tbody>
          {rows.map((e) => {
            const [text, color] = LABEL[e.type] || [e.type, 'gray']
            const showButtons = e.type === 'COURSE_DELETED' && deleted.includes(e.payload?.courseId) && !shownButtons.has(e.payload.courseId)
            if (showButtons) shownButtons.add(e.payload.courseId)
            return (
              <tr key={e.id}>
                <td>
                  <span className={`ev-label ${color}`} title="Filter by this event" onClick={() => change('type', f.type === e.type ? '' : e.type)}>{text}</span>
                  <Sentence e={e} meId={me?.id} userName={userName} />
                  {' - '}<span className="ev-time">{ago(e.occurredAt)}{e.times > 1 ? ` (${e.times} times)` : ''}</span>
                  {showButtons && <>
                    {' '}<button className="btn btn-sm btn-success" onClick={() => undo(e)}>Undo delete</button>
                    {' '}<button className="btn btn-sm btn-danger" onClick={() => purge(e)}>Permanently delete</button>
                  </>}
                </td>
              </tr>
            )
          })}
          {data && rows.length === 0 && <tr><td className="hint">No events for this filter</td></tr>}
        </tbody>
      </table>

      <div className="toolbar" style={{ marginTop: 14 }}>
        <span className="count" style={{ marginTop: 0, cursor: 'pointer' }} title="Show more/less results per page"
          onClick={() => { setSize(size === 10 ? 25 : size === 25 ? 50 : 10); setPage(0) }}>
          {total === 0 ? 0 : page * size + 1} to {Math.min(total, (page + 1) * size)} of {total} ▾
        </span>
        <div className="right">
          <button className="btn btn-light btn-sm" title="Save as CSV" onClick={exportCsv}>↓ CSV</button>
          <div className="seg">
            <button disabled={page === 0} onClick={() => setPage(page - 1)}>←</button>
            <button disabled style={{ fontWeight: 700 }}>{page + 1} / {pages}</button>
            <button disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>→</button>
          </div>
        </div>
      </div>
    </Page>
  )
}

/** Предложение события. Свои действия показываем как "You ...", как в TalentLMS. */
export function Sentence({ e, meId, userName }) {
  const p = e.payload || {}
  const mine = e.userId && e.userId === meId
  const name = (p.firstName ? `${p.firstName} ${p.lastName || ''}`.trim() : null) || userName(e.userId) || 'Someone'
  const who = <span className="ev-entity">{mine ? 'You' : name}</span>
  const course = <span className="ev-entity">{p.courseName}</span>
  const unit = <span className="ev-entity">{p.unitName}</span>
  switch (e.type) {
    case 'USER_LOGGED_IN': return <>{who} signed in</>
    case 'USER_SIGNED_UP': return <>{who} signed up</>
    case 'USER_CREATED': return <>{who} {mine ? 'were' : 'was'} added by an administrator</>
    case 'USER_IMPERSONATED': return <>{who} logged into the account of <span className="ev-entity">{p.targetName}</span></>
    case 'USER_DELETED': return <>{who} {mine ? 'were' : 'was'} deleted</>
    case 'COURSE_CREATED': return <>{who} created the course {course}</>
    case 'COURSE_UPDATED': return <>{who} updated the course {course}</>
    case 'COURSE_DELETED': return <>{who} deleted the course {course}</>
    case 'COURSE_RESTORED': return <>{who} restored the course {course}</>
    case 'USER_ENROLLED': return <>{who} {mine ? 'were' : 'was'} added to the course {course}</>
    case 'COURSE_COMPLETED': return <>{who} completed the course {course}</>
    case 'UNIT_ADDED': return <>{who} added the unit {unit} to the course {course}</>
    case 'UNIT_COMPLETED': return <>{who} completed the unit {unit} of the course {course} ({p.progress}%)</>
    case 'TEST_COMPLETED': return <>{who} {p.passed === 'true' ? 'passed' : 'failed'} the test {unit} of the course {course} ({p.score}%)</>
    case 'CERTIFICATE_ISSUED': return <>{who} received a certificate for the course {course} (no. {p.code})</>
    default: return <>{e.description}</>
  }
}
