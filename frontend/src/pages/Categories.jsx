import { useState } from 'react'
import Page from '../components/Page.jsx'
import { useApi } from '../components/useApi'
import { api } from '../api/client'

export default function Categories() {
  const { data, error, reload } = useApi('/api/categories')
  const [name, setName] = useState('')
  const [parentId, setParentId] = useState('')
  const [price, setPrice] = useState('')
  const [err, setErr] = useState(null)

  const add = async (e) => {
    e.preventDefault()
    setErr(null)
    try {
      await api.post('/api/categories', { name, parentId: parentId || null, price: price === '' ? null : Number(price) })
      setName(''); setParentId(''); setPrice(''); reload()
    } catch (ex) { setErr(ex.message) }
  }

  const remove = async (c) => {
    if (!confirm(`Delete category "${c.name}"?`)) return
    try { await api.delete(`/api/categories/${c.id}`); reload() } catch (ex) { alert(ex.message) }
  }

  const byId = Object.fromEntries((data || []).map((c) => [c.id, c]))

  return (
    <Page title="Categories">
      <form className="toolbar" onSubmit={add}>
        <input className="search" placeholder="e.g. Accounting" value={name} onChange={(e) => setName(e.target.value)} required />
        <select value={parentId} onChange={(e) => setParentId(e.target.value)} style={{ padding: 8 }}>
          <option value="">Select a parent category</option>
          {(data || []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <input className="search" style={{ width: 100 }} type="number" placeholder="Price" value={price} onChange={(e) => setPrice(e.target.value)} />
        <button className="btn btn-primary">Add category</button>
      </form>
      {(err || error) && <div className="error" style={{ marginTop: 10 }}>{err || error}</div>}
      <table className="grid">
        <thead><tr><th>Name</th><th>Parent</th><th>Price</th><th>Options</th></tr></thead>
        <tbody>
          {(data || []).map((c) => (
            <tr key={c.id}>
              <td>{c.name}</td><td>{byId[c.parentId]?.name || '-'}</td><td>{c.price ?? '-'}</td>
              <td><button className="link-btn danger" onClick={() => remove(c)}>✕ Delete</button></td>
            </tr>
          ))}
        </tbody>
      </table>
      <span className="count">1 to {data?.length ?? 0} of {data?.length ?? 0}</span>
    </Page>
  )
}
