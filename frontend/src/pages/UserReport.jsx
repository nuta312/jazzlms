import { useState } from 'react'
import { Link, NavLink, useParams } from 'react-router-dom'
import { Area, AreaChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import Page from '../components/Page.jsx'
import { fmtDate, fmtDateTime, useApi } from '../components/useApi'
import { lastLogin, TYPE_LABEL } from './Users.jsx'
import { EVENT_NAME } from './Timeline.jsx'
import { fmtDuration } from './MyProgress.jsx'
import { UserViewToggle } from './UserPage.jsx'

const PERIODS = ['today', 'yesterday', 'week', 'month', 'year']
const ordinal = (n) => n + (n % 10 === 1 && n !== 11 ? 'st' : n % 10 === 2 && n !== 12 ? 'nd' : n % 10 === 3 && n !== 13 ? 'rd' : 'th')

/**
 * Reports -> User reports -> Progress. Одна страница собирает четыре сервиса:
 * user-service (профиль), course-service (курсы), analytics-service (активность), gamification-service (очки, бейджи, ранги).
 */
export default function UserReport({ view = 'progress' }) {
  const { id } = useParams()
  const [period, setPeriod] = useState('month')
  const { data: user, error } = useApi(`/api/users/${id}`)
  const { data: enrollments } = useApi(`/api/enrollments/user/${id}`)
  const { data: activity } = useApi(`/api/analytics/users/${id}/activity?period=${period}`)
  const { data: timeline } = useApi(`/api/analytics/timeline?userId=${id}&size=15`)
  const { data: game } = useApi(`/api/gamification/users/${id}`)

  const learning = (enrollments || []).filter((e) => e.role === 'LEARNER')
  const completed = learning.filter((e) => e.completedAt).length
  const inProgress = learning.length - completed
  const pct = learning.length ? Math.round((completed / learning.length) * 100) : 0
  const trainingSeconds = 0 // per-user training time is shown on the learner's own Progress page
  const series = activity ? activity.labels.map((l, i) => ({ label: l, logins: activity.logins[i], completions: activity.completions[i] })) : []
  const title = user ? `${user.firstName} ${user.lastName}` : '…'

  const stats = (
    <div className="learner-stats" style={{ marginTop: 6 }}>
      <div><b>{inProgress}</b><span>courses in progress</span></div>
      <div><b>{completed}</b><span>courses completed</span></div>
      <div><b>{game?.badges?.length ?? 0}</b><span>badges</span></div>
      <div><b>{game?.points ?? 0}</b><span>points</span></div>
      <div><b>{ordinal(game?.level ?? 1)}</b><span>level</span></div>
    </div>
  )

  return (
    <Page crumbs={[{ to: '/reports', label: 'Reports' }, { to: '/users', label: 'User reports' }]} title={title}>
      <div className="tabs with-toggle">
        <NavLink to={`/users/${id}/report`} end>Overview</NavLink>
        <NavLink to={`/users/${id}/courses`}>Courses</NavLink>
        <a href="#" onClick={(e) => e.preventDefault()} className="disabled">Certificates</a>
        <NavLink to={`/users/${id}/infographic`}>Badges</NavLink>
        <Link to={`/timeline?userId=${id}`}>Timeline</Link>
        <UserViewToggle id={id} active={view} />
      </div>
      {error && <div className="error">{error}</div>}
      {user && (
        <>
          <div className="course-head" style={{ alignItems: 'center' }}>
            <div className="avatar big">{user.firstName[0]}{user.lastName[0]}</div>
            <div>
              <h2 style={{ margin: '4px 0' }}>{user.firstName} {user.lastName} <span className="role-pill learner sm">{TYPE_LABEL[user.userType]}</span></h2>
              <p className="hint" style={{ margin: 0 }}>{user.email}</p>
            </div>
          </div>
          {stats}

          {view === 'progress' ? (
            <>
              <div className="report-grid">
                <div>
                  <h3>Activity</h3>
                  <p className="activity-line"><b>{activity?.loginsLastWeek ?? 0}</b> login{activity?.loginsLastWeek === 1 ? '' : 's'} last week · <b>{activity?.loginsLastMonth ?? 0}</b> logins last month · last login: <b>{user.lastLoginAt ? fmtDate(user.lastLoginAt) : '-'}</b></p>
                  <div className="period">{PERIODS.map((p) => <button key={p} className={p === period ? 'active' : ''} onClick={() => setPeriod(p)}>{p[0].toUpperCase() + p.slice(1)}</button>)}</div>
                  <ResponsiveContainer width="100%" height={260}>
                    <AreaChart data={series}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="label" interval={period === 'year' ? 30 : period === 'month' ? 2 : 1} fontSize={12} />
                      <YAxis allowDecimals={false} fontSize={12} />
                      <Tooltip /><Legend />
                      <Area type="monotone" dataKey="logins" name="Logins" stroke="#0b3d91" fill="#9db4e3" />
                      <Area type="monotone" dataKey="completions" name="Course completions" stroke="#27ae60" fill="#a9dfbf" />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
                <div>
                  <div className="toolbar"><h3>Progress overview</h3><span className="right"><Link to={`/users/${id}/infographic`}>View infographic ›</Link></span></div>
                  <Donut pct={pct} />
                </div>
              </div>

              <div className="report-grid">
                <div>
                  <div className="toolbar"><h3>Recently earned</h3><span className="right"><Link to={`/users/${id}/infographic`}>View all badges ›</Link></span></div>
                  <div className="badge-row">
                    {(game?.badges || []).slice(-5).reverse().map((b) => <div className="badge-round" key={b.code}><span>{b.icon}</span><b>{b.category} {b.name}</b><small>{fmtDate(b.awardedAt)}</small></div>)}
                    {game?.badges?.length === 0 && <p className="hint">No badges yet</p>}
                  </div>
                </div>
                <div className="compared">
                  <div className="toolbar"><h3>Compared to others</h3><span className="right"><Link to="/my-progress">Gamification ›</Link></span></div>
                  <div className="rank-row">
                    <div><span className="rank-circle gold">{game?.rank ? ordinal(game.rank) : '-'}</span><small>POINTS</small></div>
                    <div><span className="rank-circle green">{game?.badgeRank ? ordinal(game.badgeRank) : '-'}</span><small>BADGES</small></div>
                    <div><span className="rank-circle blue">{game?.levelRank ? ordinal(game.levelRank) : '-'}</span><small>LEVEL</small></div>
                  </div>
                </div>
              </div>

              <h3>Recent activity</h3>
              {(timeline?.items || []).map((a) => (
                <div className="timeline-item" key={a.id}><div className="when">{fmtDateTime(a.occurredAt)}</div><div>{a.description}<span className="type">{EVENT_NAME[a.type] || a.type}</span></div></div>
              ))}
            </>
          ) : (
            <Infographic user={user} learning={learning} completed={completed} pct={pct} game={game} />
          )}
        </>
      )}
    </Page>
  )
}

function Donut({ pct }) {
  const data = [{ name: 'Completed', value: pct }, { name: 'Remaining', value: 100 - pct }]
  return (
    <div className="donut">
      <ResponsiveContainer width="100%" height={260}>
        <PieChart>
          <Pie data={data} dataKey="value" innerRadius={80} outerRadius={115} startAngle={90} endAngle={-270} stroke="none">
            <Cell fill="#a9c6f7" /><Cell fill="#dbe6ff" />
          </Pie>
        </PieChart>
      </ResponsiveContainer>
      <div className="donut-label"><span>Completed</span><b>{pct}%</b></div>
    </div>
  )
}

/** Infographic: крупные цифры + все бейджи (полученные ярко, остальные приглушены). */
function Infographic({ user, learning, completed, pct, game }) {
  const { data: rules } = useApi('/api/gamification/rules')
  const earned = new Set((game?.badges || []).map((b) => b.code))
  return (
    <>
      <div className="report-grid">
        <div>
          <h3>Learning</h3>
          <div className="stats">
            <div><b>{learning.length}</b><span>courses assigned</span></div>
            <div><b>{completed}</b><span>completed</span></div>
            <div><b>{pct}%</b><span>completion rate</span></div>
          </div>
          <h3>Engagement</h3>
          <div className="stats">
            <div><b>{game?.logins ?? 0}</b><span>login days</span></div>
            <div><b>{game?.units ?? 0}</b><span>units completed</span></div>
            <div><b>{lastLogin(user.lastLoginAt)}</b><span>last login</span></div>
          </div>
        </div>
        <div><h3>Progress overview</h3><Donut pct={pct} /></div>
      </div>
      <h3>Badges</h3>
      <div className="badges">
        {(rules?.badges || []).map((b) => (
          <div className={`badge-card ${earned.has(b.code) ? 'earned' : ''}`} key={b.code}><span className="icon">{b.icon}</span><b>{b.name}</b><small>{b.category} · {b.threshold} {b.counter}</small></div>
        ))}
      </div>
      <h3>Points history</h3>
      {(game?.history || []).slice(0, 15).map((h, i) => (
        <div className="timeline-item" key={i}><div className="when">{fmtDateTime(h.at)}</div><div>{h.action}{h.note ? ` — ${h.note}` : ''} {h.points > 0 && <b className="up">+{h.points}</b>}</div></div>
      ))}
    </>
  )
}
