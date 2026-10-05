import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import Page from '../components/Page.jsx'
import { useApi } from '../components/useApi'
import { EVENT_NAME, ReportTabs } from './Timeline.jsx'

/** Reports -> Overview: события по типам (агрегация $group в MongoDB) и топ активных (Redis ZSET). */
export default function Reports() {
  const { data, error } = useApi('/api/analytics/overview')
  const { data: users } = useApi('/api/users')
  const byType = Object.entries(data?.eventsByType || {}).map(([type, count]) => ({ name: EVENT_NAME[type] || type, count }))
    .sort((a, b) => b.count - a.count)
  const name = (id) => { const u = (users || []).find((x) => x.id === id); return u ? `${u.firstName} ${u.lastName}` : `${id.slice(0, 8)}…` }

  return (
    <Page title="Reports">
      <ReportTabs />
      {error && <div className="error">{error}</div>}
      <div className="course-layout">
        <div>
          <h3>Events by type</h3>
          <ResponsiveContainer width="100%" height={Math.max(220, byType.length * 34)}>
            <BarChart data={byType} layout="vertical" margin={{ left: 60 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis type="number" allowDecimals={false} fontSize={12} />
              <YAxis type="category" dataKey="name" width={150} fontSize={12} />
              <Tooltip />
              <Bar dataKey="count" fill="#0b3d91" />
            </BarChart>
          </ResponsiveContainer>
        </div>
        <aside className="course-side" style={{ paddingLeft: 20 }}>
          <h3>Most active users</h3>
          {(data?.topUsers || []).map((t, i) => (
            <div className="unit-row" key={t.userId}><b>{i + 1}.</b> {name(t.userId)}<span className="right hint">{t.score} events</span></div>
          ))}
        </aside>
      </div>
    </Page>
  )
}
