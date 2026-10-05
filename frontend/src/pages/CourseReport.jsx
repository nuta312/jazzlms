import { useState } from 'react'
import { Link, NavLink, useParams } from 'react-router-dom'
import { Area, AreaChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import Page from '../components/Page.jsx'
import { fmtDate, useApi } from '../components/useApi'
import { auth } from '../api/client'
import { ago, collapseLogins, LABEL, Sentence } from './Timeline.jsx'
import { fmtDuration } from './MyProgress.jsx'

const PERIODS = ['today', 'yesterday', 'week', 'month', 'year']
const secs = (s) => !s ? '-' : s >= 3600 ? `${Math.floor(s / 3600)}h ${Math.floor((s % 3600) / 60)}m` : s >= 60 ? `${Math.floor(s / 60)}m ${s % 60}s` : `${s}s`

/** Course -> Reports: Overview / Users / Unit matrix / Timeline (как в TalentLMS). */
export default function CourseReport({ tab = 'overview' }) {
  const { id } = useParams()
  const { data: course } = useApi(`/api/courses/${id}`)
  return (
    <Page crumbs={[{ to: '/courses', label: 'Courses' }, { to: `/courses/${id}`, label: course?.name || '…' }]} title="Reports">
      <div className="tabs">
        <NavLink to={`/courses/${id}/reports`} end>Overview</NavLink>
        <NavLink to={`/courses/${id}/reports/users`}>Users</NavLink>
        <NavLink to={`/courses/${id}/reports/matrix`}>Unit matrix</NavLink>
        <NavLink to={`/courses/${id}/reports/timeline`}>Timeline</NavLink>
      </div>
      {tab === 'overview' && <Overview id={id} course={course} />}
      {tab === 'users' && <UsersReport id={id} course={course} />}
      {tab === 'matrix' && <Matrix id={id} course={course} />}
      {tab === 'timeline' && <CourseTimeline id={id} />}
    </Page>
  )
}

function Overview({ id, course }) {
  const [period, setPeriod] = useState('month')
  const { data: o } = useApi(`/api/courses/${id}/report/overview`)
  const { data: act } = useApi(`/api/analytics/courses/${id}/activity?period=${period}`)
  const series = act ? act.labels.map((l, i) => ({ label: l, assignments: act.logins[i], completions: act.completions[i] })) : []
  const total = o?.assignedLearners || 0
  const pct = (n) => total ? Math.round((n / total) * 1000) / 10 : 0
  const donut = [{ name: 'Completed', value: pct(o?.completedLearners) }, { name: 'In progress', value: pct(o?.learnersInProgress) }, { name: 'Not started', value: pct(o?.notStarted) }]
  return (
    <>
      <div className="course-head" style={{ alignItems: 'center' }}><div className="thumb big">🎓</div><h2 style={{ margin: 0 }}>{course?.name}</h2></div>
      <div className="learner-stats" style={{ marginTop: 14 }}>
        <div><b>{o?.assignedLearners ?? 0}</b><span>assigned learners</span></div>
        <div><b>{o?.completedLearners ?? 0}</b><span>completed learners</span></div>
        <div><b>{o?.learnersInProgress ?? 0}</b><span>learners in progress</span></div>
        <div><b>{o?.instructors ?? 0}</b><span>instructors</span></div>
        <div><b>{fmtDuration(o?.trainingSeconds)}</b><span>training time</span></div>
      </div>
      <div className="report-grid">
        <div>
          <h3>Overall</h3>
          <div className="period">{PERIODS.map((p) => <button key={p} className={p === period ? 'active' : ''} onClick={() => setPeriod(p)}>{p[0].toUpperCase() + p.slice(1)}</button>)}</div>
          <ResponsiveContainer width="100%" height={280}>
            <AreaChart data={series}>
              <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" interval={period === 'year' ? 30 : period === 'month' ? 2 : 1} fontSize={12} /><YAxis allowDecimals={false} fontSize={12} /><Tooltip /><Legend />
              <Area type="monotone" dataKey="assignments" name="Course assignments" stroke="#0b3d91" fill="#9db4e3" />
              <Area type="monotone" dataKey="completions" name="Course completions" stroke="#27ae60" fill="#a9dfbf" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
        <div>
          <h3>Progress overview</h3>
          <div className="donut">
            <ResponsiveContainer width="100%" height={280}>
              <PieChart><Pie data={donut} dataKey="value" innerRadius={80} outerRadius={120} startAngle={90} endAngle={-270} stroke="none">
                <Cell fill="#27ae60" /><Cell fill="#a9c6f7" /><Cell fill="#dbe6ff" /></Pie><Tooltip formatter={(v) => `${v}%`} /></PieChart>
            </ResponsiveContainer>
            <div className="donut-label"><span>Not started</span><b>{pct(o?.notStarted)}%</b></div>
          </div>
          <p className="hint" style={{ textAlign: 'center' }}>completed {pct(o?.completedLearners)}% · in progress {pct(o?.learnersInProgress)}%</p>
        </div>
      </div>
    </>
  )
}

function UsersReport({ id, course }) {
  const { data, error } = useApi(`/api/courses/${id}/report/users`)
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const size = 10
  const rows = (data || []).filter((r) => r.name.toLowerCase().includes(q.toLowerCase()))
  const pages = Math.max(1, Math.ceil(rows.length / size))
  const csv = () => {
    const lines = [['user', 'email', 'role', 'progress', 'completionDate', 'seconds'], ...rows.map((r) => [r.name, r.email || '', r.role, r.progress, r.completedAt || '', r.seconds])]
    const blob = new Blob(['﻿' + lines.map((l) => l.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n')], { type: 'text/csv' })
    const url = URL.createObjectURL(blob); Object.assign(document.createElement('a'), { href: url, download: `${(course?.name || 'course').replace(/\s+/g, '_')}-users.csv` }).click(); URL.revokeObjectURL(url)
  }
  return (
    <>
      {error && <div className="error">{error}</div>}
      <table className="grid">
        <thead><tr><th>User ▾</th><th>Progress</th><th>Score</th><th>Completion date</th><th>Time</th></tr></thead>
        <tbody>
          {rows.slice(page * size, (page + 1) * size).map((r) => (
            <tr key={r.enrollmentId}>
              <td><Link to={`/users/${r.userId}/report`}>{r.name}</Link> <span className={`role-pill sm ${r.role.toLowerCase()}`}>{r.role}</span></td>
              <td>{r.role !== 'LEARNER' ? '-' : r.progress === 0 && !r.seconds ? 'Not started' : <span className="progress-pill"><span style={{ width: `${r.progress}%` }} /><em>{r.progress}%</em></span>}</td>
              <td>{r.score == null ? '-' : `${r.score}%`}</td>
              <td>{r.completedAt ? fmtDate(r.completedAt) : '-'}</td>
              <td>{secs(r.seconds)}</td>
            </tr>
          ))}
          {rows.length === 0 && <tr><td colSpan={5} className="hint">No users</td></tr>}
        </tbody>
      </table>
      <div className="toolbar" style={{ marginTop: 14 }}>
        <span className="count" style={{ marginTop: 0 }}>{rows.length ? page * size + 1 : 0} to {Math.min(rows.length, (page + 1) * size)} of {rows.length}</span>
        <div className="right">
          <button className="btn btn-light btn-sm" onClick={csv}>↓ CSV</button>
          <input className="search" placeholder="Search" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
          <div className="seg"><button disabled={page === 0} onClick={() => setPage(page - 1)}>←</button><button disabled>{page + 1} / {pages}</button><button disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>→</button></div>
        </div>
      </div>
    </>
  )
}

function Matrix({ id, course }) {
  const { data, error } = useApi(`/api/courses/${id}/report/matrix`)
  const [q, setQ] = useState('')
  const [showTime, setShowTime] = useState(false)
  const [opts, setOpts] = useState(false)
  const rows = (data?.rows || []).filter((r) => r.name.toLowerCase().includes(q.toLowerCase()))
  const csv = () => {
    const lines = [['user', ...(data?.units || []).map((u) => u.name)], ...rows.map((r) => [r.name, ...(data?.units || []).map((u) => r.cells[u.id]?.status || '')])]
    const blob = new Blob(['﻿' + lines.map((l) => l.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n')], { type: 'text/csv' })
    const url = URL.createObjectURL(blob); Object.assign(document.createElement('a'), { href: url, download: `${(course?.name || 'course').replace(/\s+/g, '_')}-matrix.csv` }).click(); URL.revokeObjectURL(url)
  }
  return (
    <>
      {error && <div className="error">{error}</div>}
      <div className="toolbar">
        <input className="search" placeholder="Search users" value={q} onChange={(e) => setQ(e.target.value)} />
        <div className="right">
          <div className="dropdown">
            <button className="btn btn-light btn-sm" onClick={() => setOpts(!opts)}>⚙ Options</button>
            {opts && <div className="dropdown-menu"><div className="menu-title">Show</div><label className="check" style={{ padding: '6px 18px' }}><input type="checkbox" checked={showTime} onChange={(e) => setShowTime(e.target.checked)} /> Unit time</label></div>}
          </div>
          <button className="btn btn-success btn-sm" onClick={csv}>↓ Export CSV</button>
        </div>
      </div>
      <div className="matrix-wrap">
        <table className="matrix">
          <thead><tr><th className="user-col">Users ▾</th>{(data?.units || []).map((u) => <th key={u.id}><div className="rot"><span>{u.name}</span></div></th>)}</tr></thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.userId}>
                <td className="user-col"><Link to={`/users/${r.userId}/report`}>{r.name}</Link></td>
                {(data?.units || []).map((u) => {
                  const c = r.cells[u.id]
                  return <td key={u.id}><div className={`cell ${c?.status === 'COMPLETED' ? 'done' : c?.status === 'STARTED' ? 'started' : ''}`} title={c ? `${c.status.toLowerCase()} · ${secs(c.seconds)}` : 'not started'}>{c?.status === 'COMPLETED' ? '✓' : c?.status === 'STARTED' ? '○' : ''}{showTime && c?.seconds ? <small>{secs(c.seconds)}</small> : null}</div></td>
                })}
              </tr>
            ))}
            {rows.length === 0 && <tr><td colSpan={99} className="hint" style={{ padding: 20 }}>No learners</td></tr>}
          </tbody>
        </table>
      </div>
      <p className="hint">1 - {rows.length} of {data?.rows?.length ?? 0} · <span className="cell done inline">✓</span> completed &nbsp; <span className="cell started inline">○</span> in progress</p>
    </>
  )
}

function CourseTimeline({ id }) {
  const me = auth.user()
  const [page, setPage] = useState(0)
  const { data } = useApi(`/api/analytics/timeline?courseId=${id}&page=${page}&size=10`)
  const { data: users } = useApi('/api/users')
  const userName = (uid) => { const u = (users || []).find((x) => x.id === uid); return u ? `${u.firstName} ${u.lastName}` : null }
  const total = data?.total ?? 0; const pages = Math.max(1, Math.ceil(total / 10))
  return (
    <>
      <table className="grid events"><thead><tr><th>Events</th></tr></thead>
        <tbody>
          {collapseLogins(data?.items).map((e) => { const [text, color] = LABEL[e.type] || [e.type, 'gray']; return (
            <tr key={e.id}><td><span className={`ev-label ${color}`}>{text}</span><Sentence e={e} meId={me?.id} userName={userName} />{' - '}<span className="ev-time">{ago(e.occurredAt)}</span></td></tr>) })}
          {data && data.items.length === 0 && <tr><td className="hint">No events for this course</td></tr>}
        </tbody></table>
      <div className="toolbar" style={{ marginTop: 14 }}>
        <span className="count" style={{ marginTop: 0 }}>{total ? page * 10 + 1 : 0} to {Math.min(total, (page + 1) * 10)} of {total}</span>
        <div className="right"><Link to={`/timeline`} className="btn btn-light btn-sm">Full timeline</Link>
          <div className="seg"><button disabled={page === 0} onClick={() => setPage(page - 1)}>←</button><button disabled>{page + 1} / {pages}</button><button disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>→</button></div></div>
      </div>
    </>
  )
}
