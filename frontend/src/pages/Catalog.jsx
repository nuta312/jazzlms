import { Link } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { useApi } from '../components/useApi'
import { api } from '../api/client'

/** Course catalog: активные, не скрытые курсы; ученик записывается сам (POST /api/courses/{id}/enroll). */
export default function Catalog() {
  const { data, error, reload } = useApi('/api/courses/catalog')
  const enroll = async (c) => { try { await api.post(`/api/courses/${c.id}/enroll`); reload() } catch (e) { alert(e.message) } }
  return (
    <Page title="Course catalog">
      {error && <div className="error">{error}</div>}
      <div className="course-cards">
        {(data || []).map(({ course: c, enrolled }) => (
          <div className="course-card" key={c.id}>
            <div className="thumb">🎓</div>
            <b>{c.name}</b>
            <small className="hint">{c.categoryName || 'General'}{c.level ? ` · ${c.level}` : ''}</small>
            <small className="hint" style={{ minHeight: 34 }}>{(c.description || '').slice(0, 90)}</small>
            {enrolled ? <Link to={`/courses/${c.id}`} className="btn btn-light btn-sm">Open course</Link>
              : <button className="btn btn-primary btn-sm" onClick={() => enroll(c)}>Get this course</button>}
          </div>
        ))}
      </div>
      {data?.length === 0 && <p className="hint">The catalog is empty.</p>}
    </Page>
  )
}
