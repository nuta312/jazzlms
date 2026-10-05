import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { api } from '../api/client'
import { useApi } from '../components/useApi'
import QuestionPreview, { Q_ICON, Q_LABEL } from '../components/QuestionPreview.jsx'

/**
 * Add / Edit Test — как в TalentLMS: слева банк вопросов (Select questions), справа шаги
 * Set question order / Set question weight / Test options и ссылки Add question.
 * Тест — это урок типа TEST: создаётся сразу (черновик, неактивный), чтобы можно было уходить на форму
 * вопроса и возвращаться, не теряя состояние. "Save and view" делает урок активным.
 */
const QUESTION_TYPES = [['MULTIPLE_CHOICE', 'Multiple choice'], ['FILL_GAP', 'Fill the gap'], ['ORDERING', 'Ordering'],
  ['DRAG_DROP', 'Drag-and-drop'], ['FREE_TEXT', 'Free text'], ['RANDOMIZED', 'Randomized'], ['IMPORT', 'Import']]

export function NewTest() {
  // /courses/:courseId/tests/new -> создаём черновик и переходим на редактирование
  const { courseId } = useParams()
  const nav = useNavigate()
  const started = useRef(false)
  const [error, setError] = useState(null)
  useEffect(() => {
    if (started.current) return
    started.current = true
    api.post(`/api/courses/${courseId}/tests`, { name: 'New test', active: false, questions: [] })
      .then((t) => nav(`/units/${t.unitId}/test/edit?draft=1`, { replace: true })).catch((e) => setError(e.message))
  }, [courseId, nav])
  return <Page title="Add Test">{error ? <div className="error">{error}</div> : <p className="hint">Creating a draft…</p>}</Page>
}

export default function TestForm() {
  const { unitId } = useParams()
  const [search] = useSearchParams()
  const nav = useNavigate()
  const draft = search.get('draft') === '1'
  const [t, setT] = useState(null)
  const [name, setName] = useState('')
  const [qs, setQs] = useState([])                 // [{id, text, type, weight}]
  const [section, setSection] = useState('select') // select | order | weight | options
  const [error, setError] = useState(null)
  const [saveMenu, setSaveMenu] = useState(false)
  const [preview, setPreview] = useState(null)

  useEffect(() => {
    api.get(`/api/units/${unitId}/test`).then((x) => {
      setT(x); setName(x.name === 'New test' && draft ? '' : x.name)
      setQs((x.questions || []).map((q) => ({ id: q.id, text: q.text, type: q.type, weight: q.weight, courseName: q.courseName })))
    }).catch((e) => setError(e.message))
  }, [unitId, draft])

  const body = (extra = {}) => ({
    name: name.trim() || 'New test', durationMinutes: t.durationMinutes || null, passScore: t.passScore,
    shuffleQuestions: t.shuffleQuestions, shuffleAnswers: t.shuffleAnswers, repetitions: t.repetitions, maxAttempts: t.maxAttempts || null,
    showCorrectAnswers: t.showCorrectAnswers, showGivenAnswers: t.showGivenAnswers, showLabels: t.showLabels, showScore: t.showScore,
    description: t.description || null, messagePassed: t.messagePassed || null, messageFailed: t.messageFailed || null,
    questions: qs.map((q) => ({ questionId: q.id, weight: q.weight })), ...extra,
  })
  const save = async (next = 'view') => {
    setError(null); setSaveMenu(false)
    if (!name.trim()) return setError('Test name is required')
    try {
      await api.put(`/api/units/${unitId}/test`, body({ active: true }))
      if (next === 'view') nav(`/units/${unitId}`)
      else if (next === 'list') nav(`/courses/${t.courseId}`)
      else nav(`/units/${unitId}/test/edit`, { replace: true })
    } catch (err) { setError(err.message) }
  }
  const cancel = async () => {
    if (draft) { try { await api.delete(`/api/units/${unitId}`) } catch { /* ignore */ } }
    nav(`/courses/${t?.courseId || ''}`)
  }
  /** Уходя на форму вопроса, сохраняем текущее состояние теста (без активации), чтобы ничего не потерять. */
  const addQuestionLink = (type) => async (e) => {
    e.preventDefault()
    try { await api.put(`/api/units/${unitId}/test`, body()) } catch (err) { return setError(err.message) }
    nav(`/courses/${t.courseId}/questions/new?type=${type}&test=${unitId}${draft ? '&draft=1' : ''}`)
  }
  const move = (i, d) => { const j = i + d; if (j < 0 || j >= qs.length) return; const a = [...qs]; [a[i], a[j]] = [a[j], a[i]]; setQs(a) }

  if (!t) return <Page title="Add Test">{error ? <div className="error">{error}</div> : <p className="hint">Loading…</p>}</Page>
  const set = (k) => (e) => setT({ ...t, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })
  return (
    <Page crumbs={[{ to: `/courses/${t.courseId}`, label: t.courseName }]} title={draft ? 'Add Test' : 'Edit Test'}>
      <div className="course-layout test-layout">
        <div>
          {error && <div className="error">{error}</div>}
          <div className="toolbar" style={{ marginBottom: 14 }}>
            <input className="search" style={{ width: 420 }} placeholder="Test name" maxLength={80} value={name} onChange={(e) => setName(e.target.value)} autoFocus />
            <span className="hint">{80 - name.length}</span>
          </div>

          {section === 'select' && <Bank courseId={t.courseId} chosen={qs} onAdd={(q) => setQs([...qs, { id: q.id, text: q.text, type: q.type, weight: 1, courseName: q.courseName }])}
            onRemove={(q) => setQs(qs.filter((x) => x.id !== q.id))} onPreview={setPreview} unitId={unitId} draft={draft} onBeforeLeave={() => api.put(`/api/units/${unitId}/test`, body())} />}

          {section !== 'select' && qs.length === 0 && <div className="empty-box">Please add some questions to the test first</div>}
          {section === 'order' && qs.length > 0 && (
            <table className="grid"><thead><tr><th style={{ width: 60 }}>#</th><th>Question</th><th style={{ width: 90 }} /></tr></thead>
              <tbody>{qs.map((q, i) => <tr key={q.id}><td>{i + 1}</td><td>{Q_ICON[q.type]} {q.text}</td>
                <td><button className="link-btn" onClick={() => move(i, -1)}>↑</button> <button className="link-btn" onClick={() => move(i, 1)}>↓</button></td></tr>)}</tbody></table>
          )}
          {section === 'weight' && qs.length > 0 && (
            <table className="grid"><thead><tr><th>Question</th><th style={{ width: 120 }}>Weight</th></tr></thead>
              <tbody>{qs.map((q, i) => <tr key={q.id}><td>{Q_ICON[q.type]} {q.text}</td>
                <td><input type="number" min="1" max="100" className="inline-num" value={q.weight} onChange={(e) => { const a = [...qs]; a[i] = { ...q, weight: Number(e.target.value) || 1 }; setQs(a) }} /></td></tr>)}</tbody></table>
          )}
          {section === 'options' && <TestOptions t={t} set={set} setT={setT} />}

          <div className="form-actions">
            <div className="dropdown split">
              <button className="btn btn-primary" onClick={() => save('view')}>Save and view</button>
              <button className="btn btn-primary caret" onClick={() => setSaveMenu(!saveMenu)}>▾</button>
              {saveMenu && <div className="dropdown-menu up-left">
                <a href="#" onClick={(e) => { e.preventDefault(); save('list') }}>and back to units list</a>
                <a href="#" onClick={(e) => { e.preventDefault(); save('edit') }}>and continue editing</a>
              </div>}
            </div>
            <span>or <button className="link-btn" onClick={cancel}>cancel</button></span>
          </div>
        </div>

        <div className="course-side">
          <SideItem icon="☑" title="Select questions" active={section === 'select'} onClick={() => setSection('select')} sub={`${qs.length} selected`} />
          <SideItem icon="↕" title="Set question order" active={section === 'order'} onClick={() => setSection('order')} />
          <SideItem icon="◔" title="Set question weight" active={section === 'weight'} onClick={() => setSection('weight')} sub={`total ${qs.reduce((s, q) => s + q.weight, 0)}`} />
          <SideItem icon="⚙" title="Test options" active={section === 'options'} onClick={() => setSection('options')} sub={`pass ${t.passScore}%${t.durationMinutes ? ` · ${t.durationMinutes} min` : ''}`} />
          <hr />
          <div className="side-item add-q">
            <div className="icon">+</div>
            <div><b>ADD QUESTION</b>
              {QUESTION_TYPES.map(([v, l]) => <a key={v} href="#" onClick={addQuestionLink(v)}>{l}</a>)}
            </div>
          </div>
        </div>
      </div>
      {preview && <QuestionPreview question={preview} onClose={() => setPreview(null)} />}
    </Page>
  )
}

/** Банк вопросов: USE / TYPE / QUESTION / OPTIONS, поиск, пагинация, "Show questions from all courses". */
function Bank({ courseId, chosen, onAdd, onRemove, onPreview, unitId, draft, onBeforeLeave }) {
  const nav = useNavigate()
  const [all, setAll] = useState(false)
  const [scopeMenu, setScopeMenu] = useState(false)
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const size = 10
  const { data, reload } = useApi(`/api/courses/${courseId}/questions?all=${all}&search=${encodeURIComponent(q)}`)
  const rows = useMemo(() => data || [], [data])
  const chosenIds = new Set(chosen.map((c) => c.id))
  const remove = async (row) => {
    if (!window.confirm(`Delete question "${row.text.slice(0, 60)}" from the bank?`)) return
    await api.delete(`/api/questions/${row.id}`); onRemove(row); reload()
  }
  const edit = async (row) => { await onBeforeLeave(); nav(`/questions/${row.id}/edit?test=${unitId}${draft ? '&draft=1' : ''}`) }
  const pages = Math.max(1, Math.ceil(rows.length / size))
  return (
    <>
      <div className="toolbar" style={{ marginBottom: 10, justifyContent: 'flex-end' }}>
        <div className="dropdown split">
          <button className="btn btn-light" onClick={() => setAll(!all)}>{all ? 'Show questions from all courses' : 'Show questions from this course'}</button>
          <button className="btn btn-light caret" style={{ borderLeft: '1px solid #ccd0d5' }} onClick={() => setScopeMenu(!scopeMenu)}>▾</button>
          {scopeMenu && <div className="dropdown-menu" style={{ left: 'auto', right: 0 }}>
            <a href="#" onClick={(e) => { e.preventDefault(); setAll(!all); setScopeMenu(false) }}>{all ? 'Show questions from this course only' : 'Show questions from all courses'}</a></div>}
        </div>
      </div>
      <table className="grid bank">
        <thead><tr><th style={{ width: 90 }}>Use</th><th style={{ width: 60 }}>Type</th><th>Question</th><th style={{ width: 110, textAlign: 'right' }}>Options</th></tr></thead>
        <tbody>
          {rows.slice(page * size, (page + 1) * size).map((row) => (
            <tr key={row.id} className={chosenIds.has(row.id) ? 'chosen' : ''}>
              <td>{chosenIds.has(row.id) ? <button className="btn btn-light btn-sm" onClick={() => onRemove(row)}>Remove</button> : <button className="btn btn-primary btn-sm" onClick={() => onAdd(row)}>Add</button>}</td>
              <td title={Q_LABEL[row.type]}>{Q_ICON[row.type]}</td>
              <td>{row.text.length > 90 ? row.text.slice(0, 90) + '…' : row.text}{all && <small className="hint"> · {row.courseName}</small>}</td>
              <td className="row-actions">
                <button className="link-btn" title="Edit" onClick={() => edit(row)}>✎</button>
                <button className="link-btn" title="Preview" onClick={() => onPreview(row)}>🔍</button>
                <button className="link-btn danger" title="Delete" onClick={() => remove(row)}>✕</button>
              </td>
            </tr>
          ))}
          {rows.length === 0 && <tr><td colSpan={4} className="hint">No questions yet. Use "Add question" on the right.</td></tr>}
        </tbody>
      </table>
      <div className="toolbar" style={{ marginTop: 12 }}>
        <span className="count" style={{ marginTop: 0 }}>{rows.length ? page * size + 1 : 0} to {Math.min(rows.length, (page + 1) * size)} of {rows.length}</span>
        <span className="right">
          <input className="search" placeholder="Search" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
          <button className="btn btn-light btn-sm" disabled={page === 0} onClick={() => setPage(page - 1)}>←</button>
          <span>{page + 1}</span>
          <button className="btn btn-light btn-sm" disabled={page >= pages - 1} onClick={() => setPage(page + 1)}>→</button>
        </span>
      </div>
    </>
  )
}

const MSG_TABS = [['description', 'Description'], ['messagePassed', 'Message (if passed)'], ['messageFailed', 'Message (if not passed)']]

/** Test options — секции как в TalentLMS; Behavior и Security показаны, но не реализованы. */
function TestOptions({ t, set, setT }) {
  const [tab, setTab] = useState('description')
  return (
    <div className="test-options">
      <label>Duration</label>
      <div className="toolbar"><input type="number" min="1" className="inline-num" placeholder="minutes" value={t.durationMinutes || ''} onChange={set('durationMinutes')} /> <span className="hint">minutes (empty = no limit)</span></div>
      <label>Pass score</label>
      <div className="toolbar"><input type="number" min="0" max="100" className="inline-num" value={t.passScore} onChange={set('passScore')} /> %</div>
      <h4>Randomization</h4>
      <label className="check-row"><input type="checkbox" checked={t.shuffleQuestions} onChange={set('shuffleQuestions')} /> Shuffle questions</label>
      <label className="check-row"><input type="checkbox" checked={t.shuffleAnswers} onChange={set('shuffleAnswers')} /> Shuffle possible answers</label>
      <h4>Repetitions</h4>
      <label className="check-row"><input type="checkbox" checked={t.repetitions !== 'NEVER'} onChange={(e) => setT({ ...t, repetitions: e.target.checked ? 'IF_NOT_PASSED' : 'NEVER' })} /> Allow repetitions
        <select disabled={t.repetitions === 'NEVER'} value={t.repetitions === 'NEVER' ? 'IF_NOT_PASSED' : t.repetitions} onChange={set('repetitions')}><option value="IF_NOT_PASSED">if not passed</option><option value="ALWAYS">always</option></select></label>
      <label className="check-row" style={{ paddingLeft: 26 }}><input type="checkbox" checked={!!t.maxAttempts} onChange={(e) => setT({ ...t, maxAttempts: e.target.checked ? 3 : null })} /> maximum number of attempts
        {t.maxAttempts ? <input type="number" min="1" className="inline-num" value={t.maxAttempts} onChange={set('maxAttempts')} /> : null}</label>
      <h4>Completion</h4>
      <label className="check-row"><input type="checkbox" checked={t.showCorrectAnswers !== 'NEVER'} onChange={(e) => setT({ ...t, showCorrectAnswers: e.target.checked ? 'WHEN_PASSED' : 'NEVER' })} /> Show correct answers
        <select disabled={t.showCorrectAnswers === 'NEVER'} value={t.showCorrectAnswers === 'NEVER' ? 'WHEN_PASSED' : t.showCorrectAnswers} onChange={set('showCorrectAnswers')}><option value="WHEN_PASSED">when passed</option><option value="ALWAYS">always</option></select></label>
      <label className="check-row"><input type="checkbox" checked={t.showGivenAnswers} onChange={set('showGivenAnswers')} /> Show given answers</label>
      <label className="check-row"><input type="checkbox" checked={t.showLabels} onChange={set('showLabels')} /> Show correct/incorrect labels</label>
      <label className="check-row"><input type="checkbox" checked={t.showScore} onChange={set('showScore')} /> Show score</label>
      <h4 className="hint">Behavior · Security</h4>
      <p className="hint">Movement between questions, learner snapshot and test password are not implemented in JazzLMS.</p>
      <div className="tabs" style={{ margin: '14px 0 10px' }}>
        {MSG_TABS.map(([k, l]) => <a key={k} href="#" className={tab === k ? 'active' : ''} onClick={(e) => { e.preventDefault(); setTab(k) }}>{l}</a>)}
      </div>
      <textarea rows={4} style={{ width: '100%', maxWidth: 720 }} maxLength={tab === 'description' ? 800 : 2000} value={t[tab] || ''} onChange={set(tab)}
        placeholder={tab === 'description' ? 'Add a test description up to 800 characters' : 'Shown to the learner after the test'} />
    </div>
  )
}

function SideItem({ icon, title, sub, active, onClick }) {
  return (
    <div className={`side-item ${active ? 'active' : ''}`} onClick={onClick}>
      <div className="icon">{icon}</div>
      <div><b>{title}</b>{sub && <><br /><small className="hint">{sub}</small></>}</div>
    </div>
  )
}
