import { Link } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { fmtDate, fmtDateTime, useApi } from '../components/useApi'
import { auth } from '../api/client'

export const fmtDuration = (sec = 0) => {
  const d = Math.floor(sec / 86400), h = Math.floor((sec % 86400) / 3600), m = Math.floor((sec % 3600) / 60)
  return d > 0 ? `${d}d ${h}h` : h > 0 ? `${h}h ${m}m` : `${m}m`
}

/** "Progress": мои курсы + игровой профиль (очки, уровень, бейджи, история начислений). */
export default function MyProgress() {
  const user = auth.user()
  const { data: enrollments } = useApi('/api/enrollments/my?role=LEARNER')
  const { data: stats } = useApi('/api/enrollments/my/stats')
  const { data: game } = useApi('/api/gamification/me')
  const { data: certs } = useApi('/api/certificates/my')
  const completed = (enrollments || []).filter((e) => e.completedAt).length
  return (
    <Page title="My progress">
      <div className="stats">
        <div><b>{(enrollments?.length ?? 0) - completed}</b><span>courses in progress</span></div>
        <div><b>{completed}</b><span>completed</span></div>
        <div><b>{fmtDuration(stats?.trainingSeconds)}</b><span>training time</span></div>
        <div><b>{game?.points ?? 0}</b><span>points{game?.rank ? ` · rank #${game.rank}` : ''}</span></div>
        <div><b>{game?.level ?? 1}</b><span>level{game?.nextLevelPoints ? ` · next at ${game.nextLevelPoints}` : ''}</span></div>
      </div>
      <h3>Badges</h3>
      <div className="badges">
        {(game?.badges || []).map((b) => <div className="badge-card earned" key={b.code}><span className="icon">{b.icon}</span><b>{b.name}</b><small>{b.category} · {fmtDate(b.awardedAt)}</small></div>)}
        {game?.badges?.length === 0 && <p className="hint">No badges yet. Sign in daily and complete units to earn them.</p>}
      </div>
      <h3>Courses</h3>
      <table className="grid">
        <thead><tr><th>Course</th><th>Progress</th><th>Completed</th></tr></thead>
        <tbody>
          {(enrollments || []).map((e) => (
            <tr key={e.enrollmentId}>
              <td><Link to={`/courses/${e.course.id}`}>{e.course.name}</Link></td>
              <td><div className="progress"><div style={{ width: `${e.progress}%` }} /></div> {e.progress}%</td>
              <td>{e.completedAt ? fmtDate(e.completedAt) : '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <h3>Certificates</h3>
      {(certs || []).length === 0 && <p className="hint">No certificates yet. Complete a course that issues one.</p>}
      {(certs || []).map((c) => (
        <div className="timeline-item" key={c.id}><div className="when">{fmtDate(c.issuedAt)}</div>
          <div>🎓 <Link to={`/certificates/${c.id}`}>{c.courseName}</Link> <small className="hint">no. {c.code}</small></div></div>
      ))}
      <h3>Points history</h3>
      {(game?.history || []).slice(0, 20).map((h, i) => (
        <div className="timeline-item" key={i}><div className="when">{fmtDateTime(h.at)}</div><div>{h.action}{h.note ? ` — ${h.note}` : ''} {h.points > 0 && <b className="up">+{h.points}</b>}</div></div>
      ))}
      {game?.history?.length === 0 && <p className="hint">Nothing yet, {user?.firstName}.</p>}
    </Page>
  )
}
