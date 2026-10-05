import { Link } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { useApi } from '../components/useApi'

export default function MyCourses() {
  const { data, error } = useApi('/api/enrollments/my')
  return (
    <Page title="My courses">
      {error && <div className="error">{error}</div>}
      {data?.length === 0 && <p className="hint">You are not enrolled in any course yet. Browse the <Link to="/courses">catalog</Link>.</p>}
      <table className="grid">
        <thead><tr><th>Course</th><th>Category</th><th>My role</th><th>Progress</th></tr></thead>
        <tbody>
          {(data || []).map((e) => (
            <tr key={e.enrollmentId}>
              <td><Link to={`/courses/${e.course.id}`}>{e.course.name}</Link></td>
              <td>{e.course.categoryName || '-'}</td>
              <td>{e.role === 'INSTRUCTOR' ? 'Instructor' : 'Learner'}</td>
              <td>{e.role === 'LEARNER' ? <><div className="progress"><div style={{ width: `${e.progress}%` }} /></div> {e.progress}%</> : '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Page>
  )
}
