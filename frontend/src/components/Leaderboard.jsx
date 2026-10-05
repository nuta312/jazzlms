import { useState } from 'react'
import { useApi } from './useApi'

const MEDAL = { 1: 'gold', 2: 'silver', 3: 'bronze' }

/** Модальное окно Leaderboard с вкладками Points / Levels / Badges и правилами ("How to collect points"). */
export default function Leaderboard({ onClose, config }) {
  // вкладки по настройкам Account & Settings → Gamification → Leaderboard (Show points / levels / badges)
  const lbc = config?.leaderboard || {}
  const tabs = [lbc.showPoints !== false && 'points', lbc.showLevels !== false && 'levels', lbc.showBadges !== false && 'badges'].filter(Boolean)
  const [by, setBy] = useState('points')
  const [showRules, setShowRules] = useState(false)
  const { data } = useApi(`/api/gamification/leaderboard?by=${by}&limit=6`)
  const { data: rules } = useApi(showRules ? '/api/gamification/rules' : null)

  const rows = data ? [...data.top, ...(data.me && !data.top.some((t) => t.me) ? [data.me] : [])] : []

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head"><h2>{showRules ? 'How to collect points' : 'Leaderboard'}</h2><button className="link-btn" onClick={onClose}>✕</button></div>
        {config?.leaderboard?.enabled === false ? <p className="hint">The leaderboard is switched off by the administrator.</p> : !showRules ? (
          <>
            <div className="tabs" style={{ margin: '0 0 6px' }}>
              {tabs.map((t) => (
                <a key={t} href="#" className={by === t ? 'active' : ''} onClick={(e) => { e.preventDefault(); setBy(t) }}>{t[0].toUpperCase() + t.slice(1)}</a>
              ))}
            </div>
            {rows.map((r) => (
              <div className={`lb-row ${r.me ? 'me' : ''}`} key={r.userId}>
                <span className={`lb-rank ${MEDAL[r.rank] || ''}`}>{r.rank}</span>
                <span className="avatar small">{r.name.split(' ').map((w) => w[0]).join('').slice(0, 2)}</span>
                <span className="lb-name">{r.name}</span>
                <b className="lb-score">{r.score}</b>
              </div>
            ))}
            {data && rows.length === 0 && <p className="hint">No points yet — sign in, complete units and courses.</p>}
            <div style={{ textAlign: 'center', marginTop: 18 }}>
              <button className="btn btn-primary" onClick={() => setShowRules(true)}>{by === 'levels' ? 'How to level up' : by === 'badges' ? 'How to earn badges' : 'How to collect points'}</button>
            </div>
          </>
        ) : (
          <div className="rules">
            <h4>Points</h4>
            {(rules?.points || []).map((p) => <div className="rule" key={p.action}><span>{p.action} <small className="hint">{p.note}</small></span><b>+{p.points}</b></div>)}
            <h4>Levels</h4>
            <div className="levels">{(rules?.levels || []).slice(0, 10).map((l) => <span key={l.level}><b>{l.level}</b>{l.points} pts</span>)}</div>
            <h4>Badges</h4>
            <div className="badges">
              {(rules?.badges || []).map((b) => (
                <div className={`badge-card ${b.earned ? 'earned' : ''}`} key={b.code} title={`${b.category}: ${b.threshold} ${b.counter}`}>
                  <span className="icon">{b.icon}</span><b>{b.name}</b><small>{b.category} · {b.threshold} {b.counter}</small>
                </div>
              ))}
            </div>
            <div style={{ textAlign: 'center', marginTop: 14 }}><button className="btn btn-light" onClick={() => setShowRules(false)}>‹ Back</button></div>
          </div>
        )}
      </div>
    </div>
  )
}
