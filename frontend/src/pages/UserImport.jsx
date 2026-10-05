import { useState } from 'react'
import { Link } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { upload } from '../api/client'

const SAMPLE = `firstName,lastName,email,username,password,userType
Aliya,Sadykova,aliya@example.com,aliya,secret123,LEARNER
Timur,Bekov,timur@example.com,timur,,TRAINER
Dana,Kim,dana@example.com,dana,secret123,`

/** Add user -> Import user(s). CSV уходит одним multipart-запросом, в ответ — отчёт по строкам. */
export default function UserImport() {
  const [file, setFile] = useState(null)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError(null); setResult(null); setBusy(true)
    try {
      const form = new FormData()
      form.append('file', file)
      setResult(await upload('/api/users/import', form))
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  const sample = () => {
    const url = URL.createObjectURL(new Blob([SAMPLE], { type: 'text/csv' }))
    Object.assign(document.createElement('a'), { href: url, download: 'users-sample.csv' }).click()
    URL.revokeObjectURL(url)
  }

  return (
    <Page crumbs={[{ to: '/users', label: 'Users' }]} title="Import user(s)">
      <p>Upload a CSV file. Columns: <code>firstName, lastName, email, username, password, userType</code>.
        Password may be empty (a random one is generated), userType defaults to LEARNER (also: TRAINER, ADMIN).
        Separator can be <code>,</code> or <code>;</code>. <button className="link-btn" onClick={sample}>Download sample file</button></p>
      <pre className="email">{SAMPLE}</pre>
      <form className="toolbar" onSubmit={submit} style={{ marginTop: 16 }}>
        <input type="file" accept=".csv,text/csv" onChange={(e) => setFile(e.target.files[0])} required />
        <button className="btn btn-primary" disabled={busy}>{busy ? 'Importing…' : 'Import'}</button>
        <span>or <Link to="/users">cancel</Link></span>
      </form>
      {error && <div className="error" style={{ marginTop: 14 }}>{error}</div>}
      {result && (
        <div style={{ marginTop: 20 }}>
          <p><span className="badge ok" style={{ fontSize: 13 }}>{result.created} created</span>
            {result.skipped.length > 0 && <span className="badge" style={{ fontSize: 13 }}>{result.skipped.length} skipped</span>}
            &nbsp; <Link to="/users">Go to users</Link></p>
          {result.skipped.length > 0 && (
            <table className="grid">
              <thead><tr><th>Line</th><th>Username</th><th>Reason</th></tr></thead>
              <tbody>{result.skipped.map((s) => <tr key={s.line}><td>{s.line}</td><td>{s.value}</td><td>{s.reason}</td></tr>)}</tbody>
            </table>
          )}
        </div>
      )}
    </Page>
  )
}
