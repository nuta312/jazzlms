import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { api, auth, upload } from '../api/client'
import { useApi } from '../components/useApi'

const TITLE = { VIDEO: 'Video', PRESENTATION: 'Presentation | Document', CONTENT: 'Content' }
const ACCEPT = { VIDEO: 'video/*', PRESENTATION: '.pdf,.ppt,.pptx,.doc,.docx,.odp,.xls,.xlsx' }
const EMPTY = { name: '', completionType: 'CHECKBOX', timeLimitSeconds: '', question: '', answer: '',
  sourceType: 'YOUTUBE', youtubeUrl: '', textContent: '', active: true, autoplay: false, showSpeed: true, description: '' }

/**
 * Форма урока (Add Video / Add Presentation / Add Content) — как в TalentLMS:
 * Unit name, How to complete it, источник. Файл уходит multipart-запросом с прогрессом.
 */
export default function UnitForm() {
  const { courseId: courseIdParam, unitId } = useParams()
  const [search] = useSearchParams()
  const nav = useNavigate()
  const [type, setType] = useState(search.get('type') || 'VIDEO')
  const [courseId, setCourseId] = useState(courseIdParam)
  const [courseName, setCourseName] = useState('')
  const [f, setF] = useState({ ...EMPTY, sourceType: (search.get('type') || 'VIDEO') === 'VIDEO' ? 'YOUTUBE' : 'UPLOAD' })
  const [file, setFile] = useState(null)
  const [docSource, setDocSource] = useState('files')   // 'files' | 'upload'
  const [sourceUnitId, setSourceUnitId] = useState('')
  const [saveMenu, setSaveMenu] = useState(false)
  const { data: myFiles } = useApi(!unitId ? `/api/units/files/${auth.user()?.id}` : null)
  const [existingFile, setExistingFile] = useState(null)
  const [percent, setPercent] = useState(null)
  const [error, setError] = useState(null)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  useEffect(() => {
    if (!unitId) return
    api.get(`/api/units/${unitId}`).then((u) => {
      setType(u.type); setCourseId(u.courseId); setExistingFile(u.fileName)
      setF({ ...EMPTY, ...u, timeLimitSeconds: u.timeLimitSeconds ?? '', question: u.question || '', answer: u.answer || '',
        youtubeUrl: u.youtubeUrl || '', textContent: u.textContent || '', sourceType: u.sourceType || 'UPLOAD', description: u.description || '' })
    }).catch((e) => setError(e.message))
  }, [unitId])

  useEffect(() => { if (courseId) api.get(`/api/courses/${courseId}`).then((c) => setCourseName(c.name)).catch(() => {}) }, [courseId])

  const submit = async (e, next = 'view') => {
    e.preventDefault()
    setError(null)
    const unit = {
      name: f.name, type, completionType: f.completionType, active: f.active,
      timeLimitSeconds: f.completionType === 'TIME' ? Number(f.timeLimitSeconds) || null : null,
      question: f.completionType === 'QUESTION' ? f.question : null,
      answer: f.completionType === 'QUESTION' ? f.answer : null,
      sourceType: type === 'CONTENT' ? null : type === 'VIDEO' ? f.sourceType : 'UPLOAD',
      youtubeUrl: type === 'VIDEO' && f.sourceType === 'YOUTUBE' ? f.youtubeUrl : null,
      textContent: type === 'CONTENT' ? f.textContent : null,
      autoplay: f.autoplay, showSpeed: f.showSpeed, description: f.description || null,
      sourceUnitId: type === 'PRESENTATION' && docSource === 'files' && sourceUnitId ? sourceUnitId : null,
    }
    try {
      let saved
      if (unitId) {
        saved = await api.put(`/api/units/${unitId}`, unit)
      } else {
        // multipart: часть "unit" — JSON, часть "file" — байты
        const form = new FormData()
        form.append('unit', new Blob([JSON.stringify(unit)], { type: 'application/json' }))
        if (file && unit.sourceType === 'UPLOAD' && !unit.sourceUnitId) form.append('file', file)
        setPercent(0)
        saved = await upload(`/api/courses/${courseId}/units`, form, setPercent)
      }
      if (next === 'another') { setF({ ...EMPTY, sourceType: f.sourceType }); setFile(null); setPercent(null) }
      else if (next === 'list') nav(`/courses/${courseId}`)
      else if (next === 'edit') nav(`/units/${saved.id}/edit`)
      else nav(`/units/${saved.id}`)
    } catch (err) { setError(err.message); setPercent(null) }
  }

  /** Deactivate / Activate: сохраняем урок с перевёрнутым флагом active и возвращаемся к списку. */
  const toggleActive = async () => {
    try {
      const u = await api.get(`/api/units/${unitId}`)
      await api.put(`/api/units/${unitId}`, { ...u, active: !u.active })
      nav(`/courses/${courseId}`)
    } catch (err) { setError(err.message) }
  }

  const needsFile = !unitId && ((type === 'PRESENTATION' && docSource === 'upload') || (type === 'VIDEO' && f.sourceType === 'UPLOAD'))
  const docFiles = (myFiles || []).filter((x) => x.type === 'PRESENTATION')

  return (
    <Page crumbs={[{ to: `/courses/${courseId}`, label: courseName || 'Course' }]} title={`${unitId ? 'Edit' : 'Add'} ${TITLE[type]}`}>
      <form className="form wide-form" onSubmit={submit}>
        {error && <div className="error">{error}</div>}
        <div className="form-row"><label>Unit name</label>
          <input type="text" placeholder="Unit name" value={f.name} onChange={set('name')} required maxLength={80} style={{ maxWidth: 620 }} /></div>
        <div className="divider" />

        <div className="form-row"><label>How to complete it</label>
          <div className="seg">
            {[['CHECKBOX', '✓ With a checkbox'], ['QUESTION', '☑ With a question'], ['TIME', '◷ After a period of time']].map(([v, l]) => (
              <button type="button" key={v} className={f.completionType === v ? 'active' : ''} onClick={() => setF({ ...f, completionType: v })}>{l}</button>
            ))}
          </div></div>
        {f.completionType === 'QUESTION' && <>
          <div className="form-row"><label>Question</label><input type="text" value={f.question} onChange={set('question')} required maxLength={500} /></div>
          <div className="form-row"><label>Correct answer</label><input type="text" value={f.answer} onChange={set('answer')} required maxLength={200} /></div>
        </>}
        {f.completionType === 'TIME' && (
          <div className="form-row"><label>Time limit</label><input type="number" min="1" placeholder="Seconds" value={f.timeLimitSeconds} onChange={set('timeLimitSeconds')} required style={{ maxWidth: 120 }} /></div>
        )}
        <div className="divider" />

        {type === 'VIDEO' && (
          <div className="form-row"><label>Select a video</label>
            <div>
              {!unitId && (
                <div className="seg">
                  <button type="button" className={f.sourceType === 'YOUTUBE' ? 'active' : ''} onClick={() => setF({ ...f, sourceType: 'YOUTUBE' })}>Use YouTube</button>
                  <button type="button" className={f.sourceType === 'UPLOAD' ? 'active' : ''} onClick={() => setF({ ...f, sourceType: 'UPLOAD' })}>Use a video</button>
                </div>
              )}
              {f.sourceType === 'YOUTUBE' && (
                <input type="text" style={{ marginTop: 10, maxWidth: 620 }} placeholder="Paste a YouTube URL here" value={f.youtubeUrl} onChange={set('youtubeUrl')} required />
              )}
            </div></div>
        )}
        {type === 'PRESENTATION' && !unitId && (
          <div className="form-row"><label>Select a document</label>
            <div>
              <div className="seg">
                <button type="button" className={docSource === 'files' ? 'active' : ''} onClick={() => setDocSource('files')}>Use a document from your files</button>
                <button type="button" className={docSource === 'upload' ? 'active' : ''} onClick={() => setDocSource('upload')}>Upload a document</button>
              </div>
              {docSource === 'files' && (
                <div style={{ marginTop: 10 }}>
                  <select value={sourceUnitId} onChange={(e) => setSourceUnitId(e.target.value)} required style={{ maxWidth: 620 }}>
                    <option value="">{docFiles.length ? 'Choose one of your uploaded documents…' : 'You have no uploaded documents yet — upload one'}</option>
                    {docFiles.map((x) => <option key={x.unitId} value={x.unitId}>{x.fileName} — unit «{x.unitName}»</option>)}
                  </select>
                  <div className="hint">The file is copied inside MinIO (server-side copy), so both units keep their own object.</div>
                </div>
              )}
            </div></div>
        )}
        {needsFile && (
          <div className="form-row"><label>{type === 'VIDEO' ? 'Upload a video' : 'Upload a document'}</label>
            <div>
              <input type="file" accept={ACCEPT[type]} onChange={(e) => setFile(e.target.files[0])} required />
              <div className="hint">{type === 'VIDEO' ? 'mp4 / webm, up to 300 MB' : 'Accepted files: pdf (shown inline), ppt, pptx, doc, docx, xls, xlsx · max 300 MB'}{file ? ` · ${(file.size / 1024 / 1024).toFixed(1)} MB` : ''}</div>
            </div></div>
        )}
        {unitId && existingFile && <div className="form-row"><label>File</label><span className="hint">{existingFile} — to replace the file, delete the unit and add a new one</span></div>}
        {type === 'CONTENT' && (
          <div className="form-row wide"><label>Content</label><textarea style={{ minHeight: 220 }} value={f.textContent} onChange={set('textContent')} required maxLength={20000} /></div>
        )}
        {type === 'VIDEO' && (
          <div className="form-row"><label /><span className="check">
            <input type="checkbox" checked={f.autoplay} onChange={set('autoplay')} /> Autoplay &nbsp;&nbsp;
            <input type="checkbox" checked={f.showSpeed} onChange={set('showSpeed')} /> Show playback speed option
          </span></div>
        )}
        <div className="form-row wide"><label>Description</label><textarea placeholder="Text shown under the unit (optional)" value={f.description} onChange={set('description')} maxLength={20000} style={{ minHeight: 90 }} /></div>
        <div className="form-row"><label /><span className="check"><input type="checkbox" checked={f.active} onChange={set('active')} /> Active</span></div>

        {percent !== null && <div className="form-row"><label>Uploading</label><div><div className="progress" style={{ width: 320 }}><div style={{ width: `${percent}%` }} /></div> {percent}%</div></div>}

        <div className="form-actions">
          <div className="dropdown split">
            <button className="btn btn-primary" disabled={percent !== null}>Save and view</button>
            <button type="button" className="btn btn-primary caret" onClick={() => setSaveMenu(!saveMenu)}>▾</button>
            {saveMenu && (
              <div className="dropdown-menu up-left">
                <a href="#" onClick={(e) => { e.preventDefault(); setSaveMenu(false); submit(e, 'list') }}>and back to units list</a>
                <a href="#" onClick={(e) => { e.preventDefault(); setSaveMenu(false); submit(e, 'edit') }}>and continue editing</a>
                {!unitId && <a href="#" onClick={(e) => { e.preventDefault(); setSaveMenu(false); submit(e, 'another') }}>and add another</a>}
              </div>
            )}
          </div>
          <span>or <a href="#" onClick={(e) => { e.preventDefault(); nav(`/courses/${courseId}`) }}>cancel</a></span>
          {unitId && <span className="right toolbar">
            <button type="button" className="btn btn-orange" onClick={() => toggleActive()}>{f.active ? 'Deactivate' : 'Activate'}</button>
            <button type="button" className="btn btn-danger" onClick={async () => { if (!confirm('Delete this unit?')) return; await api.delete(`/api/units/${unitId}`); nav(`/courses/${courseId}`) }}>🗑 Delete</button>
          </span>}
        </div>
      </form>
    </Page>
  )
}
