import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import VideoPlayer from '../components/VideoPlayer.jsx'
import TestPlayer from '../components/TestPlayer.jsx'
import { api, auth, mode } from '../api/client'

/**
 * Просмотр урока в стиле TalentLMS: своя шапка (название, назад, выбор урока, EDIT / ADD / MORE),
 * контент на всю ширину и кнопка Complete под ним.
 * При открытии — POST /start (сервер запоминает время начала), контент грузится напрямую из MinIO (presigned).
 */
export default function UnitView() {
  const { unitId } = useParams()
  const nav = useNavigate()
  const [unit, setUnit] = useState(null)
  const [siblings, setSiblings] = useState([])
  const [course, setCourse] = useState(null)
  const [error, setError] = useState(null)
  const [msg, setMsg] = useState(null)
  const [answer, setAnswer] = useState('')
  const [left, setLeft] = useState(0)
  const [menu, setMenu] = useState(null) // 'units' | 'add' | 'more'
  const barRef = useRef(null)
  const manage = ['SUPER_ADMIN', 'ADMIN', 'TRAINER'].includes(auth.user()?.userType) && mode.canManage()

  useEffect(() => {
    setUnit(null); setMsg(null); setAnswer(''); setMenu(null)
    api.post(`/api/units/${unitId}/start`).then((u) => {
      setUnit(u)
      api.get(`/api/courses/${u.courseId}`).then(setCourse).catch(() => {})
      api.get(`/api/courses/${u.courseId}/units`).then(setSiblings).catch(() => {})
    }).catch((e) => setError(e.status === 403 && e.message.includes('sequential') ? '🔒 ' + e.message : e.message))
  }, [unitId])

  useEffect(() => {
    const close = (e) => { if (barRef.current && !barRef.current.contains(e.target)) setMenu(null) }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  // обратный отсчёт для "After a period of time" (сервер всё равно перепроверит)
  useEffect(() => {
    if (!unit || unit.completionType !== 'TIME' || unit.completed) return
    const tick = () => setLeft(Math.max(0, Math.ceil(unit.timeLimitSeconds - (Date.now() - new Date(unit.startedAt).getTime()) / 1000)))
    tick(); const t = setInterval(tick, 1000); return () => clearInterval(t)
  }, [unit])

  const complete = async (e) => {
    e?.preventDefault(); setMsg(null)
    try {
      const u = await api.post(`/api/units/${unitId}/complete`, { answer })
      setUnit(u)
      if (next) setTimeout(() => nav(`/units/${next.id}`), 700)   // как в TalentLMS: после Complete — следующий урок
    } catch (err) { setMsg(err.message) }
  }
  const logout = () => { auth.clear(); nav('/login') }

  const idx = siblings.findIndex((s) => s.id === unitId)
  const prev = idx > 0 ? siblings[idx - 1] : null
  const next = idx >= 0 && idx < siblings.length - 1 ? siblings[idx + 1] : null
  const isPdf = unit?.fileContentType === 'application/pdf' || unit?.fileName?.toLowerCase().endsWith('.pdf')

  return (
    <div className="unit-page">
      <div className="unit-bar" ref={barRef}>
        <h1>{unit?.name || '…'}{unit && !unit.active && <span className="badge">inactive</span>}</h1>
        <div className="unit-nav">
          <button className="unit-back" title="Previous unit" onClick={() => nav(prev ? `/units/${prev.id}` : `/courses/${unit?.courseId}`)}>‹</button>
          <div className="dropdown">
            <button className="unit-menu-btn" onClick={() => setMenu(menu === 'units' ? null : 'units')}>{unit?.name} ▾</button>
            {menu === 'units' && (
              <div className="dropdown-menu wide">
                {siblings.map((s, i) => <Link key={s.id} to={`/units/${s.id}`} className={s.id === unitId ? 'selected' : ''} onClick={() => setMenu(null)}>{i + 1}. {s.name}{s.completed ? ' ✓' : ''}</Link>)}
              </div>
            )}
          </div>
          {manage && <>
            <Link to={unit?.type === 'TEST' ? `/units/${unitId}/test/edit` : `/units/${unitId}/edit`} className="unit-menu-btn">EDIT</Link>
            <div className="dropdown">
              <button className="unit-menu-btn" onClick={() => setMenu(menu === 'add' ? null : 'add')}>ADD ▾</button>
              {menu === 'add' && unit && (
                <div className="dropdown-menu">
                  <Link to={`/courses/${unit.courseId}/units/new?type=CONTENT`}>Content</Link><hr />
                  <Link to={`/courses/${unit.courseId}/units/new?type=VIDEO`}>Video</Link>
                  <Link to={`/courses/${unit.courseId}/units/new?type=PRESENTATION`}>Presentation | Document</Link><hr />
                  <Link to={`/courses/${unit.courseId}/tests/new`}>Test</Link>
                </div>
              )}
            </div>
          </>}
          <div className="dropdown">
            <button className="unit-menu-btn" onClick={() => setMenu(menu === 'more' ? null : 'more')}>MORE ▾</button>
            {menu === 'more' && unit && (
              <div className="dropdown-menu right">
                <Link to={`/courses/${unit.courseId}`}>Course info</Link>
                <Link to={`/courses/${unit.courseId}`}>Back to course</Link><hr />
                <a href="#" onClick={(e) => { e.preventDefault(); logout() }}>Log out</a>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="unit-body">
        {error && <div className="error">{error}</div>}
        {unit && (
          <>
            <div className="player">
              {unit.type === 'VIDEO' && unit.sourceType === 'YOUTUBE' && (
                <iframe src={unit.embedUrl + (unit.autoplay ? '?autoplay=1' : '')} title={unit.name} allow="accelerometer; autoplay; encrypted-media; picture-in-picture" allowFullScreen />
              )}
              {unit.type === 'VIDEO' && unit.sourceType === 'UPLOAD' && <VideoPlayer src={unit.fileUrl} autoplay={unit.autoplay} showSpeed={unit.showSpeed} />}
              {unit.type === 'PRESENTATION' && isPdf && <iframe src={unit.fileUrl} title={unit.name} className="doc" />}
              {unit.type === 'PRESENTATION' && !isPdf && (
                <div className="dropzone">
                  <div style={{ fontSize: 56 }}>🖥</div>
                  <p><b>{unit.fileName}</b> ({(unit.fileSize / 1024 / 1024).toFixed(1)} MB)</p>
                  <a className="btn btn-primary" href={unit.fileUrl} target="_blank" rel="noreferrer">Download presentation</a>
                  <p className="hint">Browsers can only show PDF inline. Upload a PDF export to view slides here.</p>
                </div>
              )}
              {unit.type === 'CONTENT' && <div className="text-content">{unit.textContent}</div>}
              {unit.type === 'TEST' && <TestPlayer unitId={unitId} nextUnit={next ? () => nav(`/units/${next.id}`) : null}
                onPassed={() => { setUnit({ ...unit, completed: true }); api.get(`/api/courses/${unit.courseId}/units`).then(setSiblings).catch(() => {}) }} />}
            </div>
            {unit.description && <div className="text-content unit-desc">{unit.description}</div>}

            <div className="complete-area">
              {unit.type === 'TEST' ? null : unit.completed ? <span className="badge ok" style={{ fontSize: 14, padding: '10px 16px' }}>✔ Unit completed</span> : (
                <form onSubmit={complete} className="toolbar" style={{ justifyContent: 'center' }}>
                  {unit.completionType === 'QUESTION' && <>
                    <b>{unit.question}</b>
                    <input className="search" placeholder="Your answer" value={answer} onChange={(e) => setAnswer(e.target.value)} required />
                  </>}
                  {unit.completionType === 'TIME' && left > 0 && <span className="hint">Available in {left}s</span>}
                  <button className="btn btn-complete" disabled={unit.completionType === 'TIME' && left > 0}>
                    {unit.completionType === 'QUESTION' ? 'Submit answer' : next ? 'Complete and continue' : 'Complete'}
                  </button>
                </form>
              )}
              {msg && <div className="error" style={{ display: 'inline-block', marginTop: 10 }}>{msg}</div>}
              <div className="unit-pager">
                {prev && <Link className="btn btn-light" to={`/units/${prev.id}`}>‹ {prev.name}</Link>}
                {next ? <Link className="btn btn-light" to={`/units/${next.id}`}>{next.name} ›</Link>
                  : <Link className="btn btn-light" to={`/courses/${unit.courseId}`}>Back to course</Link>}
              </div>
            </div>
          </>
        )}
      </div>
      <div className="footer" style={{ textAlign: 'center' }}>{course?.name}</div>
    </div>
  )
}
