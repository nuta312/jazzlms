import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Area, AreaChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { useApi } from '../components/useApi'
import { auth, mode } from '../api/client'
import { ago, collapseLogins, LABEL, Sentence } from './Timeline.jsx'
import { fmtDuration } from './MyProgress.jsx'

const PERIODS = ['today', 'yesterday', 'week', 'month']

/** Главная зависит от режима: Administrator / Instructor / Learner (переключатель в шапке). */
export default function Dashboard() {
  const m = mode.current()
  if (m === 'instructor') return <InstructorHome />
  if (m === 'learner') return <LearnerHome />
  return <AdminHome />
}

/* ---------------- Administrator ---------------- */

function AdminHome() {
  // три вида правой панели, как в TalentLMS: лента событий / график / таблица показателей
  const [view, setView] = useState(localStorage.getItem('jazzlms.homeView') || 'chart')
  const changeView = (v) => { setView(v); localStorage.setItem('jazzlms.homeView', v) }

  return (
    <div className="page">
      <div className="page-header">Home</div>
      <div className="dash">
        <div className="tiles">
          <Tile icon="👤" title="Users" links={[['/users', 'Users'], ['/users/new', 'Add user']]} />
          <Tile icon="📘" title="Courses" links={[['/courses', 'Courses'], ['/courses/new', 'Add course']]} />
          <Tile icon="☰" title="Categories" links={[['/categories', 'Categories']]} />
          <Tile icon="👥" title="Groups" links={[['/groups', 'Groups'], ['/groups/new', 'Add group']]} />
          <Tile icon="🏢" title="Branches" links={[['/branches', 'Branches'], ['/branches/new', 'Add branch']]} />
          <Tile icon="🎓" title="My courses" links={[['/my-courses', 'My courses']]} />
          <Tile icon="🕘" title="Events engine" links={[['/notifications', 'Notifications'], ['/notifications/new', 'Add notification']]} />
          <Tile icon="📈" title="Reports" links={[['/reports', 'Overview'], ['/timeline', 'Timeline'], ['http://localhost:3001/d/jazzlms-overview', 'Grafana logs'], ['http://localhost:9411', 'Zipkin traces']]} />
          <Tile icon="⚙️" title="Account & Settings" links={[['/settings', 'Basic settings'], ['/settings/users', 'Users'], ['/settings/certificates', 'Certificates'], ['/settings/gamification', 'Gamification']]} />
        </div>
        <div className="chart-pane">
          <div className="seg view-toggle">
            <button className={view === 'timeline' ? 'active' : ''} title="Timeline" onClick={() => changeView('timeline')}>↺</button>
            <button className={view === 'chart' ? 'active' : ''} title="Chart" onClick={() => changeView('chart')}>◿</button>
            <button className={view === 'stats' ? 'active' : ''} title="Overview" onClick={() => changeView('stats')}>▦</button>
          </div>
          {view === 'timeline' && <HomeTimeline />}
          {view === 'chart' && <HomeChart />}
          {view === 'stats' && <HomeStats />}
        </div>
      </div>
    </div>
  )
}

function HomeChart() {
  const [period, setPeriod] = useState('today')
  const { data } = useApi(`/api/analytics/dashboard?period=${period}`)
  const series = data ? data.labels.map((l, i) => ({ label: l, logins: data.logins[i], completions: data.completions[i] })) : []
  return (
    <>
          <div className="period">
            {PERIODS.map((p) => (
              <button key={p} className={p === period ? 'active' : ''} onClick={() => setPeriod(p)}>{p[0].toUpperCase() + p.slice(1)}</button>
            ))}
          </div>
          <ResponsiveContainer width="100%" height={300}>
            <AreaChart data={series}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" interval={period === 'month' ? 4 : 2} fontSize={12} />
              <YAxis allowDecimals={false} fontSize={12} />
              <Tooltip />
              <Legend />
              <Area type="monotone" dataKey="logins" name="Logins" stroke="#0b3d91" fill="#9db4e3" />
              <Area type="monotone" dataKey="completions" name="Course completions" stroke="#27ae60" fill="#a9dfbf" />
            </AreaChart>
          </ResponsiveContainer>
          {data && <p className="hint">Total: {data.totals.logins} logins, {data.totals.completions} completions</p>}
    </>
  )
}

/** Последние события одной строкой + кнопка в полный отчёт. */
function HomeTimeline() {
  const me = auth.user()
  const { data } = useApi('/api/analytics/timeline?size=30')
  const { data: users } = useApi('/api/users')
  const userName = (id) => { const u = (users || []).find((x) => x.id === id); return u ? `${u.firstName} ${u.lastName}` : null }
  return (
    <div className="home-timeline">
      {collapseLogins(data?.items).slice(0, 14).map((e) => {
        const [text, color] = LABEL[e.type] || [e.type, 'gray']
        return (
          <div className="home-event" key={e.id}>
            <span className={`ev-label ${color}`}>{text}</span>
            <span className="text"><Sentence e={e} meId={me?.id} userName={userName} /></span>
            <span className="ev-time">&nbsp;- {ago(e.occurredAt)}{e.times > 1 ? ` (${e.times} times)` : ''}</span>
          </div>
        )
      })}
      {data?.items?.length === 0 && <p className="hint">No events yet</p>}
      <Link to="/timeline" className="btn btn-primary" style={{ marginTop: 12 }}>Extended Timeline</Link>
    </div>
  )
}

/**
 * Таблица показателей. Один экран — три микросервиса: пользователи (user-service),
 * курсы и записи (course-service), активность за периоды (analytics-service: Redis + MongoDB).
 */
function HomeStats() {
  const { data: u } = useApi('/api/users/stats')
  const { data: c } = useApi('/api/courses/stats')
  const { data: s } = useApi('/api/analytics/summary')
  const sec = c?.trainingSeconds ?? 0
  const training = `${Math.floor(sec / 3600)}h ${Math.floor((sec % 3600) / 60)}m`
  return (
    <div className="home-stats">
      <h4>Overview</h4>
      <div className="stat-grid">
        <Stat value={u?.active} label="Active users" />
        <Stat value={c?.activeCourses} label="Active courses" />
        <Stat value={c?.assignedCourses} label="Assigned courses" />
        <Stat value={c?.courseCompletions} label="Course completions" />
        <Stat value={c?.inProgress} label="In progress" />
        <Stat value={training} label="Training time" />
      </div>
      {[['today', 'Today', 'yesterday'], ['week', 'Week', 'previous week']].map(([key, title, prev]) => (
        <div key={key}>
          <h4>{title}</h4>
          <div className="stat-grid">
            <Stat metric={s?.[key]?.logins} label="Logins" prev={prev} />
            <Stat metric={s?.[key]?.usersWithActivity} label="Users with activity" prev={prev} />
            <Stat metric={s?.[key]?.courseCompletions} label="Course completions" prev={prev} />
          </div>
        </div>
      ))}
    </div>
  )
}

function Stat({ value, metric, label, prev }) {
  const v = metric ? metric.value : value
  const ch = metric?.changePercent
  return (
    <div className="stat">
      <b>{v ?? '…'}</b>
      <span>{label}</span>
      {metric && <small>(<span className={ch > 0 ? 'up' : ch < 0 ? 'down' : ''}>{ch > 0 ? '+' : ''}{ch}%</span> from {prev})</small>}
    </div>
  )
}

/* ---------------- Instructor ---------------- */

function InstructorHome() {
  const isAdminType = ['SUPER_ADMIN', 'ADMIN'].includes(auth.user()?.userType)
  // преподаватель видит курсы, где он INSTRUCTOR; админ в режиме Instructor — все курсы
  const { data, error } = useApi(isAdminType ? '/api/courses' : '/api/enrollments/my?role=INSTRUCTOR')
  const [q, setQ] = useState('')
  const [collapsed, setCollapsed] = useState({})

  const courses = (data || []).map((x) => x.course || x).filter((c) => c.name.toLowerCase().includes(q.toLowerCase()))
  const groups = {}
  courses.forEach((c) => { (groups[c.categoryName || 'General'] ||= []).push(c) })

  return (
    <div className="page">
      <div className="page-header">Home</div>
      <div className="dash instructor">
        <div className="course-groups">
          <input className="search" style={{ width: 320 }} placeholder="Search my courses" value={q} onChange={(e) => setQ(e.target.value)} />
          {error && <div className="error">{error}</div>}
          {Object.keys(groups).sort().map((g) => (
            <div key={g}>
              <div className="group-bar" onClick={() => setCollapsed({ ...collapsed, [g]: !collapsed[g] })}>
                {g} <span>{collapsed[g] ? '›' : '⌄'}</span>
              </div>
              {!collapsed[g] && groups[g].map((c) => (
                <div className="group-row" key={c.id}>
                  <Link to={`/courses/${c.id}`}>{c.name}</Link> {c.code && <small className="hint">({c.code})</small>}
                  <span className="right">
                    {!c.active && <span className="badge">inactive</span>}
                    <Link to={`/courses/${c.id}`} className="badge ok">info</Link>
                  </span>
                </div>
              ))}
            </div>
          ))}
          {data && courses.length === 0 && <p className="hint" style={{ marginTop: 20 }}>No courses yet. <Link to="/courses/new">Add your first course</Link>.</p>}
        </div>
        <div className="side-tiles">
          <Tile icon="📘" title="Courses" links={[['/courses/new', 'Add course']]} />
          <Tile icon="👥" title="Users" links={[['/users', 'Users']]} />
          <Tile icon="📈" title="Reports" links={[['/timeline', 'Timeline']]} />
        </div>
      </div>
    </div>
  )
}

/* ---------------- Learner ---------------- */

const ORDERS = { name: 'Name', date: 'Date', status: 'Status' }

/**
 * Главная ученика: панель показателей, курсы по категориям с прогрессом, сортировка, вид списком/плиткой.
 * Данные из трёх сервисов: course-service (курсы, время), gamification-service (очки, уровень, бейджи), user-service (имена преподавателей по gRPC внутри course-service).
 */
function LearnerHome() {
  const { data, error } = useApi('/api/enrollments/my?role=LEARNER')
  const { data: stats } = useApi('/api/enrollments/my/stats')
  const { data: game } = useApi('/api/gamification/me')
  const [q, setQ] = useState('')
  const [order, setOrder] = useState(localStorage.getItem('jazzlms.learnerOrder') || 'date')
  const [view, setView] = useState(localStorage.getItem('jazzlms.learnerView') || 'list')
  const [orderOpen, setOrderOpen] = useState(false)
  const [collapsed, setCollapsed] = useState({})
  const pick = (k, v, setter) => { setter(v); localStorage.setItem(k, v); setOrderOpen(false) }

  const sorted = (data || [])
    .filter((e) => e.course.name.toLowerCase().includes(q.toLowerCase()))
    .sort((a, b) => order === 'name' ? a.course.name.localeCompare(b.course.name)
      : order === 'status' ? b.progress - a.progress
      : new Date(b.course.createdAt) - new Date(a.course.createdAt))
  const groups = {}
  sorted.forEach((e) => { (groups[e.course.categoryName || 'General'] ||= []).push(e) })
  const inProgress = (data || []).filter((e) => !e.completedAt).length
  const ordinal = (n) => n + (n % 10 === 1 && n !== 11 ? 'st' : n % 10 === 2 && n !== 12 ? 'nd' : n % 10 === 3 && n !== 13 ? 'rd' : 'th')

  const Row = ({ e }) => (
    <div className="group-row" key={e.enrollmentId}>
      <Link to={`/courses/${e.course.id}`}>{e.course.name}</Link>
      <span className="right">
        <span className="progress-pill" title={`${e.progress}% completed`}><span style={{ width: `${e.progress}%` }} /><em>{e.progress}%</em></span>
        {e.instructors?.length > 0 && <span className="badge instructor" title={e.instructors.join(', ')}>instructor</span>}
        <Link to={`/courses/${e.course.id}`} className="badge ok">info</Link>
      </span>
    </div>
  )

  return (
    <div className="page">
      <div className="page-header">Home</div>
      <div className="dash instructor">
        <div className="course-groups">
          <div className="toolbar">
            <input className="search" style={{ width: 320 }} placeholder="Search my courses" value={q} onChange={(e) => setQ(e.target.value)} />
            <div className="right">
              <Link to="/my-progress" className="icon-btn" data-tip="My progress">ⓘ</Link>
              <div className="dropdown">
                <button className="btn btn-light" onClick={() => setOrderOpen(!orderOpen)}>⇅ {ORDERS[order]}</button>
                {orderOpen && (
                  <div className="dropdown-menu"><div className="menu-title">Order</div>
                    {Object.entries(ORDERS).map(([v, l]) => <a key={v} href="#" className={order === v ? 'selected' : ''} onClick={(ev) => { ev.preventDefault(); pick('jazzlms.learnerOrder', v, setOrder) }}>{l}</a>)}
                  </div>
                )}
              </div>
              <div className="seg">
                <button className={view === 'list' ? 'active' : ''} title="List" onClick={() => pick('jazzlms.learnerView', 'list', setView)}>☰</button>
                <button className={view === 'grid' ? 'active' : ''} title="Grid" onClick={() => pick('jazzlms.learnerView', 'grid', setView)}>▦</button>
              </div>
            </div>
          </div>

          <div className="learner-stats">
            <div><b>{inProgress}</b><span>courses in progress</span></div>
            <div><b>{fmtDuration(stats?.trainingSeconds)}</b><span>training time</span></div>
            <div><b>{game?.badges?.length ?? 0}</b><span>badges</span></div>
            <div><b>{game?.points ?? 0}</b><span>points</span></div>
            <div><b>{ordinal(game?.level ?? 1)}</b><span>level</span></div>
          </div>

          {error && <div className="error">{error}</div>}
          {view === 'list' ? Object.keys(groups).sort().map((g) => (
            <div key={g}>
              <div className="group-bar" onClick={() => setCollapsed({ ...collapsed, [g]: !collapsed[g] })}>{g} <span>{collapsed[g] ? '›' : '⌄'}</span></div>
              {!collapsed[g] && groups[g].map((e) => <Row e={e} key={e.enrollmentId} />)}
            </div>
          )) : (
            <div className="course-cards" style={{ marginTop: 18 }}>
              {sorted.map((e) => (
                <Link to={`/courses/${e.course.id}`} className="course-card" key={e.enrollmentId}>
                  <div className="thumb">🎓</div>
                  <b>{e.course.name}</b>
                  <small className="hint">{e.course.categoryName || 'General'}</small>
                  <span className="progress-pill wide"><span style={{ width: `${e.progress}%` }} /><em>{e.progress}%</em></span>
                </Link>
              ))}
            </div>
          )}
          {data?.length === 0 && <p className="hint" style={{ marginTop: 20 }}>You are not enrolled in any course yet. Visit the <Link to="/catalog">course catalog</Link>.</p>}
        </div>
        <div className="side-tiles">
          <Tile icon="📘" title="Course catalog" links={[['/catalog', 'Find new courses']]} />
          <Tile icon="📈" title="Progress" links={[['/my-progress', 'Find out how you are doing with your training']]} />
          <Tile icon="🏆" title="Leaderboard" links={[['/my-progress', 'Points, levels and badges']]} />
        </div>
      </div>
    </div>
  )
}

function Tile({ icon, title, links }) {
  return (
    <div className="tile">
      <div className="icon">{icon}</div>
      <div>
        <h3>{title}</h3>
        <div className="links">
          {links.map(([to, label]) =>
            to.startsWith('http')
              ? <a key={to} href={to} target="_blank" rel="noreferrer">{label}</a>
              : <Link key={to} to={to}>{label}</Link>)}
        </div>
      </div>
    </div>
  )
}
