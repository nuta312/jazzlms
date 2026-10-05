import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { useApi } from '../components/useApi'
import { api } from '../api/client'
import EnrollmentsTab from '../components/EnrollmentsTab.jsx'
import { Link, NavLink } from 'react-router-dom'

const EMPTY = { name: '', code: '', description: '', categoryId: '', price: '', capacity: '', level: '', active: true, hiddenFromCatalog: false, sequential: false, certificateTemplateId: '' }

export default function CourseForm({ tab = 'course' }) {
  const { id } = useParams()
  const [menu, setMenu] = useState(null)   // 'save' | 'goto'
  const menuRef = useRef(null)
  useEffect(() => { const c = (e) => { if (menuRef.current && !menuRef.current.contains(e.target)) setMenu(null) }; document.addEventListener('mousedown', c); return () => document.removeEventListener('mousedown', c) }, [])
  const nav = useNavigate()
  const { data: categories } = useApi('/api/categories')
  const { data: templates } = useApi('/api/certificates/templates')
  const [f, setF] = useState(EMPTY)
  const [error, setError] = useState(null)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  useEffect(() => {
    if (id) api.get(`/api/courses/${id}`).then((c) => setF({ ...EMPTY, ...c, categoryId: c.categoryId || '', price: c.price ?? '', capacity: c.capacity ?? '', level: c.level || '', sequential: !!c.sequential, certificateTemplateId: c.certificateTemplateId || '' }))
  }, [id])

  const submit = async (e, next = 'index') => {
    e.preventDefault()
    setError(null)
    const body = { ...f, categoryId: f.categoryId || null, price: f.price === '' ? null : Number(f.price),
      capacity: f.capacity === '' ? null : Number(f.capacity), level: f.level || null,
      // пустой выбор: при создании -> шаблон по умолчанию (сервер), при редактировании -> без сертификата
      certificateTemplateId: f.certificateTemplateId || null, noCertificate: !!id && !f.certificateTemplateId }
    try {
      const saved = id ? await api.put(`/api/courses/${id}`, body) : await api.post('/api/courses', body)
      if (next === 'another') setF(EMPTY)
      else if (next === 'users') nav(id ? `/courses/${id}/edit/users` : `/courses/${saved.id}`)
      else if (next === 'content') nav(`/courses/${saved.id}`)
      else nav('/courses')
    } catch (err) { setError(err.message) }
  }

  const cloneCourse = async () => { try { const c = await api.post(`/api/courses/${id}/clone`); nav(`/courses/${c.id}`) } catch (err) { setError(err.message) } }
  const deleteCourse = async () => { if (!confirm(`Delete course "${f.name}"?\nYou can undo this from Reports → Timeline.`)) return; try { await api.delete(`/api/courses/${id}`); nav('/courses') } catch (err) { setError(err.message) } }

  return (
    <Page crumbs={id ? [{ to: '/courses', label: 'Courses' }, { to: `/courses/${id}`, label: f.name || 'Course' }] : [{ to: '/courses', label: 'Courses' }]} title={id ? 'Edit course' : 'Add course'}>
      {id && (
        <div className="tabs with-toggle">
          <NavLink to={`/courses/${id}/edit`} end>Course</NavLink>
          <NavLink to={`/courses/${id}/edit/users`}>Users</NavLink>
          <div className="view-toggle-tabs"><Link to={`/courses/${id}/edit`} className="active">Info</Link><Link to={`/courses/${id}/reports`}>Reports</Link></div>
        </div>
      )}
      {tab === 'users' && id ? <EnrollmentsTab courseId={id} manage /> : (
      <form className="form" onSubmit={(e) => submit(e, id ? 'content' : 'users')}>
        {error && <div className="error">{error}</div>}
        <div className="form-row"><label>Course name</label><input type="text" placeholder="e.g. Introduction to Accounting" value={f.name} onChange={set('name')} required maxLength={100} /></div>
        <div className="form-row"><label>Category</label>
          <select value={f.categoryId} onChange={set('categoryId')}>
            <option value="">Select a category</option>
            {(categories || []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select></div>
        <div className="form-row wide"><label>Description</label><textarea placeholder="Add a course description up to 5000 characters" value={f.description || ''} onChange={set('description')} /></div>
        <div className="form-row"><label /><span className="check">
          <input type="checkbox" checked={f.active} onChange={set('active')} /> Active &nbsp;&nbsp;
          <input type="checkbox" checked={f.hiddenFromCatalog} onChange={set('hiddenFromCatalog')} /> Hide from course catalog
        </span></div>
        <div className="divider" />
        <div className="form-row"><label>🏷 Course code</label><input type="text" value={f.code || ''} onChange={set('code')} maxLength={20} /></div>
        <div className="form-row"><label>🛒 Price</label><input type="number" min="0" step="0.01" value={f.price} onChange={set('price')} /></div>
        <div className="form-row"><label>👥 Capacity</label><input type="number" min="1" value={f.capacity} onChange={set('capacity')} /></div>
        <div className="form-row"><label>ⓘ Level</label>
          <select value={f.level} onChange={set('level')}>
            <option value="">-</option><option>Beginner</option><option>Intermediate</option><option>Advanced</option>
          </select></div>
        <div className="form-row"><label /><span className="check"><input type="checkbox" checked={f.sequential} onChange={set('sequential')} /> Sequential rule set (Rules &amp; path)</span></div>
        <div className="form-row"><label>Certificate</label>
          <select value={f.certificateTemplateId} onChange={set('certificateTemplateId')}>
            <option value="">{id ? 'No certificate' : 'Default template'}</option>
            {(templates || []).map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select></div>
        <div className="form-actions" ref={menuRef}>
          <div className="dropdown split">
            <button className="btn btn-primary">{id ? 'Update course' : 'Save and select users'}</button>
            <button type="button" className="btn btn-primary caret" onClick={() => setMenu(menu === 'save' ? null : 'save')}>▾</button>
            {menu === 'save' && (
              <div className="dropdown-menu">
                <a href="#" onClick={(e) => { e.preventDefault(); submit(e, 'another') }}>and add another</a>
                <a href="#" onClick={(e) => { e.preventDefault(); submit(e, 'users') }}>and go to users</a>
                <a href="#" onClick={(e) => { e.preventDefault(); submit(e, 'index') }}>and go to course index</a>
              </div>
            )}
          </div>
          <span>or <a href="/courses" onClick={(e) => { e.preventDefault(); nav(id ? `/courses/${id}` : '/courses') }}>cancel</a></span>
          {id && (
            <div className="dropdown split right">
              <Link to={`/courses/${id}`} className="btn btn-primary">↗ Go to course content</Link>
              <button type="button" className="btn btn-primary caret" onClick={() => setMenu(menu === 'goto' ? null : 'goto')}>▾</button>
              {menu === 'goto' && (
                <div className="dropdown-menu right">
                  <a href="#" onClick={(e) => { e.preventDefault(); cloneCourse() }}>⧉ Clone</a>
                  <a href="#" onClick={(e) => { e.preventDefault(); deleteCourse() }}>🗑 Delete</a>
                </div>
              )}
            </div>
          )}
        </div>
      </form>
      )}
    </Page>
  )
}
