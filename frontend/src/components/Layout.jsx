import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { auth, impersonation, mode, MODE_LABEL } from '../api/client'
import Leaderboard from './Leaderboard.jsx'
import { useApi } from './useApi'

export default function Layout() {
  const nav = useNavigate()
  const user = auth.user()
  const [open, setOpen] = useState(false)
  const [current, setCurrent] = useState(mode.current())
  const [lb, setLb] = useState(false)
  const { data: game, reload: reloadGame } = useApi('/api/gamification/me')
  const { data: gcfg } = useApi('/api/gamification/config')   // GAMIFICATION ON/OFF и вкладки таблицы лидеров
  const menuRef = useRef(null)

  useEffect(() => {
    const close = (e) => { if (menuRef.current && !menuRef.current.contains(e.target)) setOpen(false) }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  const logout = () => { auth.clear(); nav('/login') }
  const original = impersonation.original()
  const returnToAdmin = () => { impersonation.stop(); nav('/users'); nav(0) }
  const switchMode = (m) => { mode.set(m); setCurrent(m); setOpen(false); nav('/') }

  return (
    <>
      {original && (
        <div className="impersonation-bar">
          You are logged in as <b>{user?.firstName} {user?.lastName}</b> (by {original.user.firstName} {original.user.lastName}).
          <button className="link-btn" onClick={returnToAdmin}>Return to my account</button>
        </div>
      )}
      <header className="topbar">
        <Link to="/" className="logo"><span className="mark">J</span> jazzlms</Link>
        <nav>
          {gcfg?.enabled !== false && <button className="points-btn" title="Leaderboard" onClick={() => { reloadGame(); setLb(true) }}>{game?.points ?? 0} POINTS</button>}
          <div className="user-menu" ref={menuRef}>
            <button className="user-menu-btn" onClick={() => setOpen(!open)}>
              {user?.firstName?.[0]}. {user?.lastName} | <span className="role">{MODE_LABEL[current]}</span> ▾
            </button>
            {open && (
              <div className="user-menu-pop">
                <div className="modes">
                  {mode.available().map((m) => (
                    <label key={m} className={m === current ? 'active' : ''}>
                      <input type="radio" name="mode" checked={m === current} onChange={() => switchMode(m)} /> {MODE_LABEL[m]}
                    </label>
                  ))}
                </div>
                <Link to={`/users/${user?.id}/edit`} onClick={() => setOpen(false)} style={{ display: mode.isAdmin() ? 'block' : 'none' }}>🪪 My info</Link>
                <Link to="/my-courses" onClick={() => setOpen(false)}>📘 My courses</Link>
                <Link to="/timeline" onClick={() => setOpen(false)} style={{ display: current === 'learner' ? 'none' : 'block' }}>📈 Timeline</Link>
                <Link to="/settings" onClick={() => setOpen(false)} style={{ display: mode.isAdmin() ? 'block' : 'none' }}>⚙️ Account &amp; Settings</Link>
                <Link to="/change-password" onClick={() => setOpen(false)}>🔑 Change password</Link>
              </div>
            )}
          </div>
          <NavLink to="/courses">Courses</NavLink>
          {/* key={current}: смена режима перерисовывает страницу */}
          <span className="logout" title="Logout" onClick={logout}>⎋</span>
        </nav>
      </header>
      {lb && <Leaderboard onClose={() => setLb(false)} config={gcfg} />}
      <Outlet key={current} />
      <div className="footer">JazzLMS 0.1.0 · Java 21 · Spring Boot · Kafka · gRPC · Redis · PostgreSQL · MongoDB · MinIO</div>
    </>
  )
}
