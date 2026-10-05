import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { api } from '../api/client'
import { useApi } from '../components/useApi'
import { Q_ICON, Q_LABEL } from '../components/QuestionPreview.jsx'

const EMPTY = {
  MULTIPLE_CHOICE: { answers: [{ text: '', correct: false }, { text: '', correct: false }] },
  FILL_GAP: {},
  ORDERING: { items: ['', ''] },
  DRAG_DROP: { pairs: [{ left: '', right: '' }, { left: '', right: '' }] },
  FREE_TEXT: { threshold: 1, options: [{ mode: 'contains', word: '', points: 1 }] },
  RANDOMIZED: { pool: [] },
}
const AIKEN_EXAMPLE = `What is the entry point of a Java program?
A. The first class in the file
B. The main() method
C. The constructor
ANSWER: B

Which keyword creates an object?
A. class
B. new
C. this
ANSWER: B`

/**
 * Add / Edit question. Один компонент на все типы: форма меняется по type,
 * а на сервер уходит {type, text, data} — data и есть JSONB-колонка questions.data.
 * ?test=<unitId>: после сохранения вопрос добавляется в этот тест.
 */
export default function QuestionForm() {
  const { courseId: courseIdParam, id } = useParams()
  const [search] = useSearchParams()
  const nav = useNavigate()
  const testId = search.get('test')
  const draft = search.get('draft') === '1'
  const [type, setType] = useState(search.get('type') || 'MULTIPLE_CHOICE')
  const [courseId, setCourseId] = useState(courseIdParam)
  const [text, setText] = useState('')
  const [data, setData] = useState(EMPTY[search.get('type')] || EMPTY.MULTIPLE_CHOICE)
  const [feedback, setFeedback] = useState('')
  const [tags, setTags] = useState('')
  const [open, setOpen] = useState({})
  const [error, setError] = useState(null)
  const [saveMenu, setSaveMenu] = useState(false)
  // Import
  const [importData, setImportData] = useState('')
  const [parsed, setParsed] = useState(null)

  useEffect(() => {
    if (!id) return
    api.get(`/api/questions/${id}`).then((q) => { setType(q.type); setCourseId(q.courseId); setText(q.text); setData(q.data || {}); setFeedback(q.feedback || ''); setTags(q.tags || '') })
      .catch((e) => setError(e.message))
  }, [id])

  const back = () => nav(testId ? `/units/${testId}/test/edit${draft ? '?draft=1' : ''}` : `/courses/${courseId}`)
  /** Добавить сохранённый вопрос в тест: читаем состав, дописываем, сохраняем (без активации). */
  const attachToTest = async (qid) => {
    if (!testId) return
    const t = await api.get(`/api/units/${testId}/test`)
    if ((t.questions || []).some((q) => q.id === qid)) return
    await api.put(`/api/units/${testId}/test`, { ...t, questions: [...(t.questions || []).map((q) => ({ questionId: q.id, weight: q.weight })), { questionId: qid, weight: 1 }] })
  }
  const save = async (next = 'back') => {
    setError(null); setSaveMenu(false)
    try {
      if (type === 'IMPORT') {
        const created = await api.post(`/api/courses/${courseId}/questions/import`, { data: importData })
        for (const q of created) await attachToTest(q.id)
        return back()
      }
      const body = { type, text, data, feedback: feedback || null, tags: tags || null }
      const q = id ? await api.put(`/api/questions/${id}`, body) : await api.post(`/api/courses/${courseId}/questions`, body)
      await attachToTest(q.id)
      if (next === 'another') { setText(''); setData(EMPTY[type]); setFeedback(''); return }
      back()
    } catch (err) { setError(err.message) }
  }
  const previewImport = async () => {
    setError(null)
    try { setParsed(await api.post(`/api/courses/${courseId}/questions/import?dryRun=true`, { data: importData })) } catch (err) { setError(err.message); setParsed(null) }
  }
  const upd = (patch) => setData({ ...data, ...patch })
  const title = `${id ? 'Edit' : 'Add'} question (${type === 'IMPORT' ? 'Import' : Q_LABEL[type]})`

  return (
    <Page crumbs={[{ to: `/courses/${courseId}`, label: 'Course' }, ...(testId ? [{ to: `/units/${testId}/test/edit`, label: 'Test' }] : [])]} title={title}>
      {error && <div className="error">{error}</div>}
      {type === 'IMPORT' ? (
        <div className="form" style={{ maxWidth: 1000 }}>
          <div className="form-row"><label>Type</label><span className="with-info"><select value="AIKEN" readOnly><option>AIKEN</option></select><span className="info" title="Question, options A./B./C., then ANSWER: letter. Blank line between questions.">i</span></span></div>
          <div className="form-row"><label>Data</label><div>
            <button type="button" className="btn btn-light btn-sm" onClick={() => setImportData(AIKEN_EXAMPLE)}>Example</button>
            <textarea rows={10} style={{ width: '100%', marginTop: 8, fontFamily: 'monospace' }} value={importData} onChange={(e) => { setImportData(e.target.value); setParsed(null) }} /></div></div>
          {parsed && <div className="form-row"><label>Preview</label><div>{parsed.map((p, i) => <div key={i} className="q-opt">☑ {p.text} <small className="hint">({p.data.answers.length} options)</small></div>)}</div></div>}
          <div className="form-actions">
            <button className="btn btn-primary" onClick={() => save()}>Save</button>
            <button className="btn btn-light" onClick={previewImport}>Preview</button>
            <span>or <button className="link-btn" onClick={back}>cancel</button></span>
          </div>
        </div>
      ) : (
        <div className="form" style={{ maxWidth: 1000 }}>
          {type === 'RANDOMIZED'
            ? <div className="toolbar"><input className="search" style={{ width: 420 }} placeholder="Question name" maxLength={80} value={text} onChange={(e) => setText(e.target.value)} /><span className="hint">{80 - text.length}</span></div>
            : <>
              <div className="q-toolbar hint">Plain text editor · {type === 'FILL_GAP' ? 'use [brackets] for the gaps' : 'no formatting in JazzLMS'}</div>
              <textarea className="q-editor" rows={7} value={text} onChange={(e) => setText(e.target.value)} placeholder="Question text" />
            </>}

          {type === 'MULTIPLE_CHOICE' && <>
            <h4>Answers</h4>
            {data.answers.map((a, i) => (
              <div className="q-row" key={i}>
                <input type="text" placeholder={`Answer ${i + 1}`} value={a.text} onChange={(e) => { const arr = [...data.answers]; arr[i] = { ...a, text: e.target.value }; upd({ answers: arr }) }} />
                <label className="check"><input type="checkbox" checked={!!a.correct} onChange={(e) => { const arr = [...data.answers]; arr[i] = { ...a, correct: e.target.checked }; upd({ answers: arr }) }} /> Correct</label>
                {data.answers.length > 2 && <button className="link-btn" title="Remove" onClick={() => upd({ answers: data.answers.filter((_, j) => j !== i) })}>🗑</button>}
              </div>))}
            <button className="btn btn-light btn-sm" onClick={() => upd({ answers: [...data.answers, { text: '', correct: false }] })}>Add answer</button>
          </>}
          {type === 'FILL_GAP' && (
            <p className="hint" style={{ marginTop: 10 }}>Note: Compose the question and use [brackets] for the possible answers. For example, <i>The quick brown [fox] jumps over the lazy [dog].</i> OR
              <i> The [biggest|bigger] planet of our solar system is [jupiter|neptune|earth].</i> When you use | to offer multiple answers the first should be the correct one.</p>
          )}
          {type === 'ORDERING' && <>
            <p className="hint" style={{ marginTop: 10 }}>Note: Add possible answers in the correct order. We'll present them randomly for the end-user.</p>
            {data.items.map((x, i) => (
              <div className="q-row" key={i}><input type="text" placeholder={`Answer ${i + 1}`} value={x} onChange={(e) => { const arr = [...data.items]; arr[i] = e.target.value; upd({ items: arr }) }} />
                {data.items.length > 2 && <button className="link-btn" onClick={() => upd({ items: data.items.filter((_, j) => j !== i) })}>🗑</button>}</div>))}
            <button className="btn btn-light btn-sm" onClick={() => upd({ items: [...data.items, ''] })}>Add answer</button>
          </>}
          {type === 'DRAG_DROP' && <>
            <p className="hint" style={{ marginTop: 10 }}>Note: Add the matching pairs. The right column is shuffled for the end-user.</p>
            {data.pairs.map((p, i) => (
              <div className="q-row" key={i}>
                <input type="text" placeholder={`Pair ${i + 1}`} value={p.left} onChange={(e) => { const arr = [...data.pairs]; arr[i] = { ...p, left: e.target.value }; upd({ pairs: arr }) }} />
                <input type="text" placeholder={`Pair ${i + 1}`} value={p.right} onChange={(e) => { const arr = [...data.pairs]; arr[i] = { ...p, right: e.target.value }; upd({ pairs: arr }) }} />
                {data.pairs.length > 2 && <button className="link-btn" onClick={() => upd({ pairs: data.pairs.filter((_, j) => j !== i) })}>🗑</button>}
              </div>))}
            <button className="btn btn-light btn-sm" onClick={() => upd({ pairs: [...data.pairs, { left: '', right: '' }] })}>Add pair</button>
          </>}
          {type === 'FREE_TEXT' && <>
            <div className="q-row" style={{ marginTop: 14 }}>Consider correct when accumulated points are greater or equal to <input type="number" className="inline-num" min="0" value={data.threshold} onChange={(e) => upd({ threshold: Number(e.target.value) })} /></div>
            {data.options.map((o, i) => (
              <div className="q-row" key={i}>When
                <select value={o.mode} onChange={(e) => { const arr = [...data.options]; arr[i] = { ...o, mode: e.target.value }; upd({ options: arr }) }}><option value="contains">contains</option><option value="not_contains">does not contain</option><option value="equals">equals</option></select>
                the word <input type="text" placeholder="e.g., fast | quick" value={o.word} onChange={(e) => { const arr = [...data.options]; arr[i] = { ...o, word: e.target.value }; upd({ options: arr }) }} />
                add <input type="number" className="inline-num" min="0" value={o.points} onChange={(e) => { const arr = [...data.options]; arr[i] = { ...o, points: Number(e.target.value) }; upd({ options: arr }) }} /> points
                {data.options.length > 1 && <button className="link-btn" onClick={() => upd({ options: data.options.filter((_, j) => j !== i) })}>🗑</button>}
              </div>))}
            <button className="btn btn-light btn-sm" onClick={() => upd({ options: [...data.options, { mode: 'contains', word: '', points: 1 }] })}>Add option</button>
          </>}
          {type === 'RANDOMIZED' && <Pool courseId={courseId} pool={data.pool} setPool={(pool) => upd({ pool })} />}

          <div className="settings-links" style={{ marginLeft: 0 }}>
            <button type="button" className="settings-link" onClick={() => setOpen({ ...open, fb: !open.fb })}>💬 Feedback</button>
            {open.fb && <div className="settings-sub"><textarea rows={3} placeholder="Shown with the correct answer after the test" value={feedback} onChange={(e) => setFeedback(e.target.value)} /></div>}
            <button type="button" className="settings-link" onClick={() => setOpen({ ...open, tg: !open.tg })}>🏷 Tags</button>
            {open.tg && <div className="settings-sub"><input type="text" style={{ width: 400 }} placeholder="comma, separated, tags" value={tags} onChange={(e) => setTags(e.target.value)} /></div>}
          </div>
          <div className="form-actions">
            <div className="dropdown split">
              <button className="btn btn-primary" onClick={() => save()}>Save</button>
              <button className="btn btn-primary caret" onClick={() => setSaveMenu(!saveMenu)}>▾</button>
              {saveMenu && <div className="dropdown-menu up-left">{!id && <a href="#" onClick={(e) => { e.preventDefault(); save('another') }}>and add another</a>}<a href="#" onClick={(e) => { e.preventDefault(); save() }}>and back</a></div>}
            </div>
            <span>or <button className="link-btn" onClick={back}>cancel</button></span>
          </div>
        </div>
      )}
    </Page>
  )
}

/** Randomized: выбор вопросов в пул из банка (кроме других Randomized). */
function Pool({ courseId, pool, setPool }) {
  const [all, setAll] = useState(false)
  const { data } = useApi(`/api/courses/${courseId}/questions?all=${all}`)
  const rows = (data || []).filter((q) => q.type !== 'RANDOMIZED')
  const has = (id) => pool.includes(id)
  return (
    <>
      <div className="toolbar" style={{ margin: '12px 0' }}><span className="hint">Each time this question is shown, it will use a random question from the selected pool</span>
        <button className="btn btn-light btn-sm right" onClick={() => setAll(!all)}>{all ? 'Show questions from this course' : 'Show questions from all courses'}</button></div>
      <table className="grid bank"><thead><tr><th style={{ width: 90 }}>Use</th><th>Question</th><th style={{ width: 60 }}>Type</th></tr></thead>
        <tbody>{rows.map((q) => <tr key={q.id} className={has(q.id) ? 'chosen' : ''}>
          <td>{has(q.id) ? <button className="btn btn-light btn-sm" onClick={() => setPool(pool.filter((x) => x !== q.id))}>Remove</button> : <button className="btn btn-primary btn-sm" onClick={() => setPool([...pool, q.id])}>Add</button>}</td>
          <td>{q.text.slice(0, 90)}</td><td>{Q_ICON[q.type]}</td></tr>)}
        {rows.length === 0 && <tr><td colSpan={3} className="hint">No questions in the bank yet</td></tr>}</tbody></table>
    </>
  )
}
