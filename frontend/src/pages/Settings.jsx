import { useEffect, useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import Page from '../components/Page.jsx'
import { api, auth } from '../api/client'
import { useApi } from '../components/useApi'

/**
 * Account & Settings, как в TalentLMS. Вкладки, у которых есть смысл в учебном проекте, работают:
 *   Basic settings, Users  -> user-service   (PostgreSQL, /api/settings)
 *   Certificates           -> course-service (PostgreSQL, /api/certificates/templates)
 *   Gamification           -> gamification-service (MongoDB, /api/gamification/settings)
 * Каждый сервис хранит свои настройки сам — это и есть "владение данными" в микросервисах.
 * Homepage / Themes / E-commerce / Domain / Subscription / New interface — только заголовки.
 */
const TABS = [
  ['basic', 'Basic settings', '/settings'], ['homepage', 'Homepage'], ['users', 'Users', '/settings/users'], ['themes', 'Themes'],
  ['certificates', 'Certificates', '/settings/certificates'], ['gamification', 'Gamification', '/settings/gamification'],
  ['ecommerce', 'E-commerce'], ['domain', 'Domain'], ['subscription', 'Subscription'], ['newui', 'New interface'],
]

export default function Settings({ tab = 'basic' }) {
  return (
    <Page title="Account & Settings">
      <div className="tabs">
        {TABS.map(([key, label, to]) => to
          ? <NavLink key={key} to={to} end className={({ isActive }) => (isActive ? 'active' : '')}>{label}</NavLink>
          : <a key={key} className="disabled" title="Not implemented in JazzLMS">{label}</a>)}
      </div>
      {tab === 'basic' && <BasicTab />}
      {tab === 'users' && <UsersTab />}
      {tab === 'certificates' && <CertificatesTab />}
      {tab === 'gamification' && <GamificationTab />}
    </Page>
  )
}

/* ---------------- Basic settings + Users (user-service) ---------------- */

function useSettingsForm() {
  const nav = useNavigate()
  const { data } = useApi('/api/settings')
  const [f, setF] = useState(null)
  const [msg, setMsg] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => { if (data) setF(data) }, [data])
  const set = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })
  const save = async (e) => {
    e.preventDefault(); setError(null); setMsg(null)
    try {
      const body = { ...f,
        passwordMaxAgeDays: f.passwordMaxAgeDays === '' || f.passwordMaxAgeDays == null ? null : Number(f.passwordMaxAgeDays),
        lockAfterAttempts: f.lockAfterAttempts === '' || f.lockAfterAttempts == null ? null : Number(f.lockAfterAttempts),
        lockMinutes: Number(f.lockMinutes || 30), defaultGroupId: f.defaultGroupId || null }
      setF(await api.put('/api/settings', body)); setMsg('Settings saved')
    } catch (err) { setError(err.message) }
  }
  return { f, set, setF, save, msg, error, cancel: () => nav('/') }
}

function BasicTab() {
  const { f, set, save, msg, error, cancel } = useSettingsForm()
  if (!f) return <p className="hint">Loading…</p>
  return (
    <form className="form" onSubmit={save}>
      {error && <div className="error">{error}</div>}{msg && <div className="notice">{msg}</div>}
      <div className="form-row"><label>Site name</label><input type="text" value={f.siteName} onChange={set('siteName')} required /></div>
      <div className="form-row"><label>Site description</label><textarea value={f.siteDescription || ''} onChange={set('siteDescription')} /></div>
      <div className="form-row"><label>Default language</label>
        <select value={f.defaultLanguage} onChange={set('defaultLanguage')}><option value="en">English</option><option value="ru">Русский</option><option value="kk">Қазақша</option></select></div>
      <div className="form-row"><label>Default time zone</label>
        <select value={f.defaultTimeZone} onChange={set('defaultTimeZone')}>{['UTC', 'Asia/Almaty', 'Asia/Bishkek', 'Europe/Moscow', 'Europe/Berlin'].map((z) => <option key={z}>{z}</option>)}</select></div>
      <div className="form-actions"><button className="btn btn-primary">Save</button><span>or <button type="button" className="link-btn" onClick={cancel}>cancel</button></span></div>
    </form>
  )
}

const USER_TYPES = [['LEARNER', 'Learner-Type'], ['TRAINER', 'Instructor-Type'], ['ADMIN', 'Admin-Type']]

function UsersTab() {
  const { f, set, setF, save, msg, error, cancel } = useSettingsForm()
  const { data: groups } = useApi('/api/groups')
  const [open, setOpen] = useState({})
  const toggle = (k) => setOpen({ ...open, [k]: !open[k] })
  if (!f) return <p className="hint">Loading…</p>
  const maxAge = f.passwordMaxAgeDays != null && f.passwordMaxAgeDays !== ''
  const lock = f.lockAfterAttempts != null && f.lockAfterAttempts !== ''
  return (
    <form className="form" onSubmit={save}>
      {error && <div className="error">{error}</div>}{msg && <div className="notice">{msg}</div>}
      <div className="form-row"><label>Signup</label>
        <select value={f.signupMode} onChange={set('signupMode')}><option value="MANUAL">Manually (from Admin)</option><option value="DIRECT">Direct</option></select></div>
      <div className="form-row"><label>Default user type</label>
        <span className="with-info"><select value={f.defaultUserType} onChange={set('defaultUserType')}>{USER_TYPES.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select>
          <Info text="Role given to users who sign up themselves" /></span></div>
      <div className="form-row"><label>Default group</label>
        <span className="with-info"><select value={f.defaultGroupId || ''} onChange={set('defaultGroupId')}><option value="">Select a group</option>
          {(groups || []).map((g) => <option key={g.group.id} value={g.group.id}>{g.group.name}</option>)}</select>
          <Info text="Every new user is added to this group automatically" /></span></div>

      <h4 className="settings-h">🔒 Password settings</h4>
      <div className="settings-box">
        <label className="check-row"><input type="checkbox" checked={maxAge} onChange={(e) => setF({ ...f, passwordMaxAgeDays: e.target.checked ? 90 : '' })} />
          Enforce password change after <input type="number" min="1" className="inline-num" disabled={!maxAge} value={maxAge ? f.passwordMaxAgeDays : ''} onChange={set('passwordMaxAgeDays')} /> days</label>
        <label className="check-row"><input type="checkbox" checked={f.passwordChangeOnFirstLogin} onChange={set('passwordChangeOnFirstLogin')} />
          Enforce password change on first login <Info text="Applies to users created by an administrator" /></label>
        <label className="check-row"><input type="checkbox" checked={lock} onChange={(e) => setF({ ...f, lockAfterAttempts: e.target.checked ? 3 : '' })} />
          Lock account after <input type="number" min="1" className="inline-num" disabled={!lock} value={lock ? f.lockAfterAttempts : ''} onChange={set('lockAfterAttempts')} /> failed attempts
          &nbsp;for <input type="number" min="1" className="inline-num" disabled={!lock} value={f.lockMinutes} onChange={set('lockMinutes')} /> minutes
          <Info text="Failed attempts are counted in Redis with a TTL; the account unlocks itself when the key expires" /></label>
      </div>

      <div className="settings-links">
        <button type="button" className="settings-link" onClick={() => toggle('tos')}>📄 Terms of Service</button>
        {open.tos && <div className="settings-sub"><p className="hint">Shown on the Sign up page; users must accept it. Leave empty to disable.</p>
          <textarea rows={6} value={f.termsOfService || ''} onChange={set('termsOfService')} /></div>}
        <button type="button" className="settings-link" onClick={() => toggle('fmt')}>👤 Visible user format</button>
        {open.fmt && <div className="settings-sub">
          <select value={f.visibleUserFormat} onChange={set('visibleUserFormat')}>
            <option value="FIRST_LAST">First name Last name</option><option value="LAST_FIRST">Last name First name</option><option value="USERNAME">Username</option></select></div>}
        <button type="button" className="settings-link" onClick={() => toggle('social')}>💬 Social options</button>
        {open.social && <div className="settings-sub hint">Not implemented in JazzLMS (Facebook / LinkedIn login).</div>}
        <button type="button" className="settings-link" onClick={() => toggle('sso')}>☁ Single Sign-On (SSO)</button>
        {open.sso && <div className="settings-sub hint">Not implemented: JazzLMS uses its own JWT login. Exercise idea: add OAuth2 / Keycloak.</div>}
      </div>
      <div className="form-actions"><button className="btn btn-primary">Save</button><span>or <button type="button" className="link-btn" onClick={cancel}>cancel</button></span></div>
    </form>
  )
}

function Info({ text }) { return <span className="info" title={text}>i</span> }

/* ---------------- Certificates (course-service) ---------------- */

function CertificatesTab() {
  const { data: templates, reload } = useApi('/api/certificates/templates')
  const { data: backgrounds } = useApi('/api/certificates/templates/backgrounds')
  const [selected, setSelected] = useState(null)
  const [f, setF] = useState(null)
  const [sub, setSub] = useState('background')
  const [preview, setPreview] = useState(null)
  const [msg, setMsg] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!templates?.length) return
    const t = templates.find((x) => x.id === selected) || templates[0]
    setSelected(t.id); setF({ name: t.name, background: t.background, title: t.title, body: t.body, signature: t.signature || '' })
  }, [templates, selected])

  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })
  const run = async (fn, ok) => { setError(null); setMsg(null); try { await fn(); reload(); if (ok) setMsg(ok) } catch (err) { setError(err.message) } }
  const update = () => run(() => api.put(`/api/certificates/templates/${selected}`, f), 'Template updated')
  const saveAsNew = () => run(async () => {
    const name = window.prompt('Name of the new template', f.name + ' (copy)'); if (!name) return
    const t = await api.post('/api/certificates/templates', { ...f, name }); setSelected(t.id)
  }, 'Template created')
  const reset = () => run(() => api.post(`/api/certificates/templates/${selected}/reset`), 'Template reset to default')
  const del = () => run(async () => { await api.delete(`/api/certificates/templates/${selected}`); setSelected(null) }, 'Template deleted')
  const showPreview = async () => {
    // сохраняем изменения формы, потом просим у course-service HTML (с токеном) и показываем в iframe
    await update()
    const res = await fetch(`/api/certificates/templates/${selected}/preview`, { headers: { Authorization: `Bearer ${auth.token()}` } })
    setPreview(await res.text())
  }
  const current = templates?.find((t) => t.id === selected)
  if (!f) return <p className="hint">Loading…</p>
  return (
    <div className="form" style={{ maxWidth: 1100 }}>
      {error && <div className="error">{error}</div>}{msg && <div className="notice">{msg}</div>}
      <div className="form-row"><label>Certificate</label>
        <select value={selected || ''} onChange={(e) => setSelected(e.target.value)}>{(templates || []).map((t) => <option key={t.id} value={t.id}>{t.name}{t.default ? ' (default)' : ''}</option>)}</select></div>
      <div className="tabs" style={{ margin: '10px 0 16px' }}>
        {['background', 'template'].map((s) => <a key={s} href="#" className={sub === s ? 'active' : ''} onClick={(e) => { e.preventDefault(); setSub(s) }}>{s[0].toUpperCase() + s.slice(1)}</a>)}
      </div>
      {sub === 'background' && (
        <div className="cert-grid">
          <div className="cert-card upload" title="Not implemented: backgrounds are drawn with CSS presets"><span>⬆ Upload your own<br />background</span></div>
          {(backgrounds || []).map((b) => (
            <div key={b} className={`cert-card ${f.background === b ? 'selected' : ''}`} onClick={() => setF({ ...f, background: b })}>
              <div className={`cert-thumb bg-${b}`} /><small>{b}</small>
            </div>
          ))}
        </div>
      )}
      {sub === 'template' && (
        <>
          <div className="form-row"><label>Name</label><input type="text" value={f.name} onChange={set('name')} /></div>
          <div className="form-row"><label>Title</label><input type="text" value={f.title} onChange={set('title')} /></div>
          <div className="form-row"><label>Body</label><textarea rows={4} value={f.body} onChange={set('body')} /></div>
          <div className="form-row"><label /><span className="hint">Placeholders: {'{user}'}, {'{course}'}, {'{date}'}, {'{code}'}</span></div>
          <div className="form-row"><label>Signature</label><input type="text" value={f.signature} onChange={set('signature')} /></div>
        </>
      )}
      <div className="form-actions">
        <button className="btn btn-primary" type="button" onClick={showPreview}>Preview</button>
        <button className="btn btn-primary" type="button" onClick={update}>Update</button>
        <button className="btn btn-primary" type="button" onClick={saveAsNew}>Save as new</button>
        <span style={{ marginLeft: 'auto' }} />
        <button className="btn btn-light" type="button" onClick={reset}>Reset to default template</button>
        <button className="btn btn-light" type="button" onClick={del} disabled={current?.default}>🗑 Delete</button>
      </div>
      {preview && (
        <div className="modal-backdrop" onClick={() => setPreview(null)}>
          <div className="modal" style={{ width: 1020 }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-head"><h2>Preview</h2><button className="link-btn" onClick={() => setPreview(null)}>✕</button></div>
            <iframe title="preview" srcDoc={preview} style={{ width: '100%', height: 560, border: 0 }} />
          </div>
        </div>
      )}
    </div>
  )
}

/* ---------------- Gamification (gamification-service) ---------------- */

function Toggle({ on, onChange, label }) {
  return (
    <button type="button" className={`switch ${on ? 'on' : ''}`} onClick={() => onChange(!on)} aria-pressed={on}>
      <span className="knob" />{label || (on ? 'ON' : 'OFF')}
    </button>
  )
}

function GamificationTab() {
  const { data, reload } = useApi('/api/gamification/settings')
  const [s, setS] = useState(null)
  const [msg, setMsg] = useState(null)
  const [error, setError] = useState(null)
  const nav = useNavigate()
  useEffect(() => { if (data) setS(data) }, [data])
  if (!s) return <p className="hint">Loading…</p>

  const patch = (section, changes) => setS({ ...s, [section]: { ...s[section], ...changes } })
  const rule = (section, key, changes) => patch(section, { [key]: { ...s[section][key], ...changes } })
  const run = async (fn, ok) => { setError(null); setMsg(null); try { await fn(); if (ok) setMsg(ok) } catch (err) { setError(err.message) } }
  const save = () => run(async () => setS(await api.put('/api/gamification/settings', s)), 'Gamification settings saved')
  const resetDefaults = () => run(async () => setS(await api.post('/api/gamification/settings/reset')), 'Default settings restored')
  const resetStats = () => run(async () => {
    if (!window.confirm('Delete all points, levels, badges and leaderboards of all users?')) return
    await api.post('/api/gamification/reset-statistics'); reload()
  }, 'Statistics reset: everyone starts from zero')

  const PointRow = ({ k, label }) => (
    <label className="check-row"><input type="checkbox" checked={s.points[k].enabled} onChange={(e) => rule('points', k, { enabled: e.target.checked })} />
      {label} <input type="number" min="0" className="inline-num" value={s.points[k].points} onChange={(e) => rule('points', k, { points: Number(e.target.value) })} /> points</label>
  )
  const LevelRow = ({ k, unit }) => (
    <label className="check-row"><input type="checkbox" checked={s.levels[k].enabled} onChange={(e) => rule('levels', k, { enabled: e.target.checked })} />
      Upgrade level every <input type="number" min="1" className="inline-num" value={s.levels[k].every} onChange={(e) => rule('levels', k, { every: Number(e.target.value) })} /> {unit}</label>
  )
  const Check = ({ section, k, label }) => (
    <label className="check-row"><input type="checkbox" checked={s[section][k]} onChange={(e) => patch(section, { [k]: e.target.checked })} /> {label}</label>
  )

  return (
    <div className="gami">
      {error && <div className="error">{error}</div>}{msg && <div className="notice">{msg}</div>}
      <div style={{ textAlign: 'right' }}><Toggle on={s.enabled} onChange={(v) => setS({ ...s, enabled: v })} label={s.enabled ? 'GAMIFICATION ON' : 'GAMIFICATION OFF'} /></div>

      <div className="gami-bar">POINTS <Toggle on={s.points.enabled} onChange={(v) => patch('points', { enabled: v })} /></div>
      <PointRow k="login" label="Each login gives" /><PointRow k="unit" label="Each unit completion gives" />
      <PointRow k="course" label="Each course completion gives" /><PointRow k="certificate" label="Each certificate gives" />
      <PointRow k="test" label="Each successful test completion gives" />
      <p className="hint">Assignments, ILT and discussions do not exist in JazzLMS, so there are no rules for them.</p>

      <div className="gami-bar">BADGES <Toggle on={s.badges.enabled} onChange={(v) => patch('badges', { enabled: v })} /></div>
      <Check section="badges" k="activity" label="Activity badges (1, 5, 25 logins)" />
      <Check section="badges" k="learning" label="Learning badges (1, 10, 50 completed units)" />
      <Check section="badges" k="certification" label="Certification badges (1, 5 completed courses)" />

      <div className="gami-bar">LEVELS <Toggle on={s.levels.enabled} onChange={(v) => patch('levels', { enabled: v })} /></div>
      <LevelRow k="byPoints" unit="points" /><LevelRow k="byCourses" unit="completed courses" /><LevelRow k="byBadges" unit="badges" />
      <p className="hint">The level is the highest one reached by any enabled rule; the cap is 20.</p>

      <div className="gami-bar muted">REWARDS <Toggle on={false} onChange={() => {}} /></div>
      <p className="hint">Discounts for course purchases need E-commerce, which JazzLMS does not have.</p>

      <div className="gami-bar">LEADERBOARD <Toggle on={s.leaderboard.enabled} onChange={(v) => patch('leaderboard', { enabled: v })} /></div>
      <Check section="leaderboard" k="showLevels" label="Show levels" />
      <Check section="leaderboard" k="showPoints" label="Show points" />
      <Check section="leaderboard" k="showBadges" label="Show badges" />

      <div className="form-actions">
        <button className="btn btn-primary" onClick={save}>Save</button><span>or <button className="link-btn" onClick={() => nav('/')}>cancel</button></span>
        <span style={{ marginLeft: 'auto' }} />
        <button className="btn btn-orange" onClick={resetDefaults}>Reset to default settings</button>
        <button className="btn btn-orange" onClick={resetStats}>Reset statistics</button>
      </div>
    </div>
  )
}
