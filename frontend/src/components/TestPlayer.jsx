import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { fmtDateTime } from './useApi'

/**
 * Прохождение теста учеником: интро (описание, попытки) -> вопросы с таймером -> результат.
 * Правильные ответы приходят только ПОСЛЕ отправки и только если это разрешено в Test options.
 */
export default function TestPlayer({ unitId, onPassed, nextUnit }) {
  const [info, setInfo] = useState(null)
  const [attempt, setAttempt] = useState(null)
  const [answers, setAnswers] = useState({})
  const [left, setLeft] = useState(null)
  const [error, setError] = useState(null)
  const load = () => api.get(`/api/units/${unitId}/test`).then(setInfo).catch((e) => setError(e.message))
  useEffect(() => { setAttempt(null); setAnswers({}); load() }, [unitId])   // eslint-disable-line react-hooks/exhaustive-deps

  const start = async () => {
    setError(null)
    try { const a = await api.post(`/api/units/${unitId}/test/attempts`); setAttempt(a); setAnswers({}) } catch (e) { setError(e.message) }
  }
  const submit = async () => {
    setError(null)
    try {
      const r = await api.post(`/api/units/${unitId}/test/attempts/${attempt.id}/submit`, { answers })
      setAttempt(r); load()
      if (r.passed) onPassed?.()
    } catch (e) { setError(e.message) }
  }
  // таймер: сервер прислал deadline, клиент отправляет тест сам, когда время вышло
  useEffect(() => {
    if (!attempt || attempt.submittedAt || !attempt.deadline) return
    const tick = () => {
      const s = Math.max(0, Math.ceil((new Date(attempt.deadline).getTime() - Date.now()) / 1000))
      setLeft(s); if (s === 0) submit()
    }
    tick(); const t = setInterval(tick, 1000); return () => clearInterval(t)
  }, [attempt])   // eslint-disable-line react-hooks/exhaustive-deps

  if (error && !info) return <div className="error">{error}</div>
  if (!info) return <p className="hint">Loading…</p>

  // ---------- результат ----------
  if (attempt?.submittedAt) {
    const byId = Object.fromEntries((attempt.results || []).map((r) => [r.questionId, r]))
    return (
      <div className="test-box">
        <div className={`test-result ${attempt.passed ? 'ok' : 'fail'}`}>
          <h2>{attempt.passed ? '✔ Test passed' : '✖ Test not passed'}</h2>
          {attempt.showScore && <p>Your score: <b>{attempt.score}%</b> (pass score {attempt.passScore}%)</p>}
          {attempt.message && <p className="text-content">{attempt.message}</p>}
        </div>
        {attempt.questions.map((q, i) => {
          const r = byId[q.questionId] || {}
          return (
            <div className={`test-q ${r.correct === true ? 'right' : r.correct === false ? 'wrong' : ''}`} key={q.questionId}>
              <div className="test-q-head">{i + 1}. {q.type === 'FILL_GAP' ? q.text.replace(/\{\{\d+}}/g, '___') : q.text}
                {r.correct === true && <span className="badge ok">correct</span>}{r.correct === false && <span className="badge">incorrect</span>}</div>
              {'given' in r && <div className="hint">Your answer: {fmtGiven(q, r.given)}</div>}
              {r.correctAnswer && <div className="hint">Correct answer: {Array.isArray(r.correctAnswer) ? r.correctAnswer.join(' · ') : String(r.correctAnswer)}</div>}
              {r.feedback && <div className="hint">💬 {r.feedback}</div>}
            </div>
          )
        })}
        <div className="toolbar" style={{ justifyContent: 'center', marginTop: 20 }}>
          {attempt.passed && nextUnit && <button className="btn btn-complete" onClick={nextUnit}>Continue to the next unit</button>}
          {!attempt.passed && info.canStart && <button className="btn btn-primary" onClick={start}>Try again</button>}
          {!attempt.passed && !info.canStart && <span className="hint">No more attempts allowed.</span>}
        </div>
      </div>
    )
  }

  // ---------- вопросы ----------
  if (attempt) {
    const set = (qid, v) => setAnswers({ ...answers, [qid]: v })
    return (
      <div className="test-box">
        <div className="test-head">
          <span>{attempt.questions.length} question(s)</span>
          {left != null && <span className={`timer ${left < 60 ? 'urgent' : ''}`}>⏱ {Math.floor(left / 60)}:{String(left % 60).padStart(2, '0')}</span>}
        </div>
        {error && <div className="error">{error}</div>}
        {attempt.questions.map((q, i) => <QuestionInput key={q.questionId} index={i} q={q} value={answers[q.questionId]} onChange={(v) => set(q.questionId, v)} />)}
        <div className="toolbar" style={{ justifyContent: 'center', marginTop: 20 }}><button className="btn btn-primary" onClick={submit}>Submit test</button></div>
      </div>
    )
  }

  // ---------- интро ----------
  return (
    <div className="test-box">
      {error && <div className="error">{error}</div>}
      {info.description && <p className="text-content">{info.description}</p>}
      <div className="stats">
        <div><b>{info.questionCount}</b><span>questions</span></div>
        <div><b>{info.passScore}%</b><span>pass score</span></div>
        <div><b>{info.durationMinutes ? `${info.durationMinutes} min` : '∞'}</b><span>duration</span></div>
        <div><b>{info.bestScore ?? '-'}</b><span>best score{info.maxAttempts ? ` · ${info.attempts.filter((a) => a.submittedAt).length}/${info.maxAttempts} attempts` : ''}</span></div>
      </div>
      {info.attempts?.length > 0 && (
        <table className="grid"><thead><tr><th>Attempt</th><th>Score</th><th>Result</th></tr></thead>
          <tbody>{info.attempts.map((a) => <tr key={a.id}><td>{fmtDateTime(a.startedAt)}</td><td>{a.score ?? '-'}%</td><td>{a.submittedAt ? (a.passed ? <span className="badge ok">passed</span> : <span className="badge">failed</span>) : <span className="badge gray">in progress</span>}</td></tr>)}</tbody></table>
      )}
      <div className="toolbar" style={{ justifyContent: 'center', marginTop: 20 }}>
        {info.openAttemptId ? <button className="btn btn-primary" onClick={start}>Continue the test</button>
          : info.canStart ? <button className="btn btn-primary" onClick={start}>{info.attempts?.length ? 'Start again' : 'Start test'}</button>
          : <span className="hint">{info.passed ? 'You have already passed this test.' : 'No more attempts allowed.'}</span>}
      </div>
    </div>
  )
}

function fmtGiven(q, given) {
  if (given == null || given === '') return '(no answer)'
  const byId = (list) => Object.fromEntries((list || []).map((o) => [o.id, o.text]))
  switch (q.type) {
    case 'MULTIPLE_CHOICE': case 'ORDERING': { const m = byId(q.options); return (Array.isArray(given) ? given : [given]).map((id) => m[id]).join(' · ') }
    case 'DRAG_DROP': { const l = byId(q.lefts), r = byId(q.rights); return Object.entries(given).map(([a, b]) => `${l[a]} → ${r[b]}`).join(' · ') }
    case 'FILL_GAP': return (given || []).join(' · ')
    default: return String(given)
  }
}

/** Ввод ответа по типу вопроса. Значение answers[questionId]: MC/ORDERING — массив id, FILL_GAP — массив строк, DRAG_DROP — {leftId: rightId}, FREE_TEXT — строка. */
function QuestionInput({ index, q, value, onChange }) {
  const head = <div className="test-q-head">{index + 1}. {q.type !== 'FILL_GAP' && q.text} {q.weight > 1 && <small className="hint">· weight {q.weight}</small>}</div>
  switch (q.type) {
    case 'MULTIPLE_CHOICE': {
      const sel = value || []
      return <div className="test-q">{head}{q.options.map((o) => (
        <label key={o.id} className="q-opt"><input type={q.multiple ? 'checkbox' : 'radio'} name={q.questionId} checked={sel.includes(o.id)}
          onChange={(e) => onChange(q.multiple ? (e.target.checked ? [...sel, o.id] : sel.filter((x) => x !== o.id)) : [o.id])} /> {o.text}</label>))}
        {q.multiple && <small className="hint">Several answers may be correct</small>}</div>
    }
    case 'FILL_GAP': {
      const vals = value || q.gaps.map(() => '')
      const parts = q.text.split(/(\{\{\d+}})/)
      return <div className="test-q"><div className="test-q-head">{index + 1}. {parts.map((p, i) => {
        const m = p.match(/^\{\{(\d+)}}$/)
        if (!m) return <span key={i}>{p}</span>
        const g = q.gaps[Number(m[1])]
        const setV = (v) => { const a = [...vals]; a[g.id] = v; onChange(a) }
        return g.choices ? <select key={i} className="gap" value={vals[g.id]} onChange={(e) => setV(e.target.value)}><option value="">…</option>{g.choices.map((c) => <option key={c}>{c}</option>)}</select>
          : <input key={i} className="gap" value={vals[g.id]} onChange={(e) => setV(e.target.value)} placeholder="…" />
      })}</div></div>
    }
    case 'ORDERING': {
      const order = value || q.options.map((o) => o.id)
      const text = Object.fromEntries(q.options.map((o) => [o.id, o.text]))
      const move = (i, d) => { const j = i + d; if (j < 0 || j >= order.length) return; const a = [...order]; [a[i], a[j]] = [a[j], a[i]]; onChange(a) }
      return <div className="test-q">{head}<ol className="ordering">{order.map((id, i) => <li key={id}><span>{text[id]}</span><button className="link-btn" onClick={() => move(i, -1)}>↑</button><button className="link-btn" onClick={() => move(i, 1)}>↓</button></li>)}</ol>
        <small className="hint">Put the items in the correct order with the arrows</small></div>
    }
    case 'DRAG_DROP': {
      const map = value || {}
      return <div className="test-q">{head}{q.lefts.map((l) => (
        <div className="q-row" key={l.id}><span style={{ minWidth: 220 }}>{l.text}</span> →
          <select value={map[l.id] ?? ''} onChange={(e) => onChange({ ...map, [l.id]: e.target.value === '' ? undefined : Number(e.target.value) })}><option value="">…</option>{q.rights.map((r) => <option key={r.id} value={r.id}>{r.text}</option>)}</select></div>))}</div>
    }
    default:
      return <div className="test-q">{head}<textarea rows={3} style={{ width: '100%' }} value={value || ''} onChange={(e) => onChange(e.target.value)} placeholder="Your answer" /></div>
  }
}
