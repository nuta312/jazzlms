import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { auth } from '../api/client'

/** Сертификат: course-service отдаёт готовый HTML, показываем его в iframe; печать — кнопкой внутри. */
export default function CertificateView() {
  const { id } = useParams()
  const [html, setHtml] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => {
    fetch(`/api/certificates/${id}/view`, { headers: { Authorization: `Bearer ${auth.token()}` } })
      .then((r) => (r.ok ? r.text() : Promise.reject(new Error(`HTTP ${r.status}`)))).then(setHtml).catch((e) => setError(e.message))
  }, [id])
  const openTab = () => { const w = window.open('', '_blank'); w.document.write(html); w.document.close() }
  return (
    <Page crumbs={[{ to: '/my-progress', label: 'My progress' }]} title="Certificate">
      {error && <div className="error">{error}</div>}
      {html && <><p><button className="btn btn-light btn-sm" onClick={openTab}>Open in a new tab</button></p>
        <iframe title="certificate" srcDoc={html} style={{ width: '100%', height: 760, border: '1px solid var(--border)', background: '#e9ebef' }} /></>}
    </Page>
  )
}
