import { Link } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { useApi } from '../components/useApi'
import { api, download } from '../api/client'

/** Одна страница-список на ветки и группы: отличаются только ресурсом и колонками. */
export default function Branches({ kind = 'branches' }) {
  const { data, error, reload } = useApi(`/api/${kind}`)
  const key = kind === 'branches' ? 'branch' : 'group'
  const one = kind === 'branches' ? 'branch' : 'group'
  const Title = kind === 'branches' ? 'Branches' : 'Groups'
  const remove = async (x) => {
    if (!confirm(`Delete ${one} "${x.name}"?`)) return
    try { await api.delete(`/api/${kind}/${x.id}`); reload() } catch (e) { alert(e.message) }
  }
  const csv = () => {
    const rows = [['name', 'description', 'active', 'members'], ...(data || []).map((r) => [r[key].name, r[key].description || '', r[key].active, r.members])]
    const blob = new Blob(['﻿' + rows.map((r) => r.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n')], { type: 'text/csv' })
    const url = URL.createObjectURL(blob); Object.assign(document.createElement('a'), { href: url, download: `${kind}.csv` }).click(); URL.revokeObjectURL(url)
  }
  return (
    <Page title={Title}>
      <div className="toolbar">
        <Link to={`/${kind}/new`} className="btn btn-primary">Add {one}</Link>
      </div>
      {error && <div className="error">{error}</div>}
      <table className="grid hover-actions">
        <thead><tr><th>Name ▾</th><th>Description</th><th>Members</th><th>Options</th></tr></thead>
        <tbody>
          {(data || []).map((r) => (
            <tr key={r[key].id}>
              <td><Link to={`/${kind}/${r[key].id}/edit`}>{r[key].name}</Link>{!r[key].active && <span className="badge">inactive</span>}</td>
              <td>{r[key].description || '-'}</td>
              <td>{r.members}</td>
              <td className="options"><span className="dots">•••</span>
                <span className="row-actions">
                  <Link to={`/${kind}/${r[key].id}/edit`} data-tip="Edit">✎</Link>
                  <button onClick={() => remove(r[key])} data-tip="Delete">✕</button>
                </span>
              </td>
            </tr>
          ))}
          {data?.length === 0 && <tr><td colSpan={4} className="hint">No {kind} yet</td></tr>}
        </tbody>
      </table>
      <div className="toolbar" style={{ marginTop: 14 }}>
        <span className="count" style={{ marginTop: 0 }}>{data?.length ? 1 : 0} to {data?.length ?? 0} of {data?.length ?? 0}</span>
        <div className="right"><button className="icon-btn" data-tip="Save as CSV" onClick={csv}>⤓</button></div>
      </div>
    </Page>
  )
}
