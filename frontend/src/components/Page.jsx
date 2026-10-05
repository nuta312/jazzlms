import { Link } from 'react-router-dom'

/** Синяя "шапка" страницы с хлебными крошками, как в TalentLMS. */
export default function Page({ crumbs = [], title, children }) {
  return (
    <div className="page">
      <div className="page-header">
        <Link to="/">Home</Link>
        {crumbs.map((c) => (
          <span key={c.to}><span className="sep">/</span><Link to={c.to}>{c.label}</Link></span>
        ))}
        <span className="sep">/</span>{title}
      </div>
      <div className="page-body">{children}</div>
    </div>
  )
}
