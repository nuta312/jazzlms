import { useEffect, useRef, useState } from 'react'

const SPEEDS = [2, 1.5, 1.25, 1, 0.5]
const fmt = (s) => { if (!isFinite(s)) return '00:00'; const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = Math.floor(s % 60); return (h ? `${String(h).padStart(2, '0')}:` : '') + `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}` }

/**
 * Собственный плеер над <video> (как в TalentLMS): большая кнопка Play, перемотка, скорость 0.5–2x,
 * громкость, полноэкранный режим. Видео идёт напрямую из MinIO по presigned-ссылке, Range-запросы
 * позволяют перематывать. Родные controls браузера выключены — рисуем свои.
 */
export default function VideoPlayer({ src, autoplay = false, showSpeed = true }) {
  const video = useRef(null)
  const wrap = useRef(null)
  const [playing, setPlaying] = useState(false)
  const [time, setTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [speed, setSpeed] = useState(1)
  const [volume, setVolume] = useState(1)
  const [muted, setMuted] = useState(false)
  const [menu, setMenu] = useState(null) // 'speed' | 'volume'

  useEffect(() => { setPlaying(false); setTime(0); setDuration(0) }, [src])

  const toggle = () => { const v = video.current; if (v.paused) v.play(); else v.pause() }
  const seek = (e) => { const r = e.currentTarget.getBoundingClientRect(); video.current.currentTime = ((e.clientX - r.left) / r.width) * duration }
  const changeSpeed = (s) => { video.current.playbackRate = s; setSpeed(s); setMenu(null) }
  const changeVolume = (v) => { video.current.volume = v; video.current.muted = v === 0; setVolume(v); setMuted(v === 0) }
  const toggleMute = () => { const m = !muted; video.current.muted = m; setMuted(m) }
  const fullscreen = () => { const el = wrap.current; document.fullscreenElement ? document.exitFullscreen() : el.requestFullscreen?.() }

  return (
    <div className="vp" ref={wrap} onMouseLeave={() => setMenu(null)}>
      <video ref={video} src={src} autoPlay={autoplay} playsInline
        onPlay={() => setPlaying(true)} onPause={() => setPlaying(false)}
        onTimeUpdate={(e) => setTime(e.target.currentTime)} onLoadedMetadata={(e) => setDuration(e.target.duration)}
        onClick={toggle} />
      {!playing && <button className="vp-big" onClick={toggle} title="Play">▶</button>}
      <div className="vp-bar">
        <button className="vp-btn" onClick={toggle} title={playing ? 'Pause' : 'Play'}>{playing ? '❚❚' : '▶'}</button>
        <div className="vp-seek" onClick={seek}><div style={{ width: `${duration ? (time / duration) * 100 : 0}%` }} /></div>
        {showSpeed && (
          <div className="vp-menu-wrap">
            <button className="vp-btn" onClick={() => setMenu(menu === 'speed' ? null : 'speed')} title="Speed Rate">{speed}x</button>
            {menu === 'speed' && <div className="vp-menu">{SPEEDS.map((s) => <button key={s} className={s === speed ? 'active' : ''} onClick={() => changeSpeed(s)}>{s}x</button>)}</div>}
          </div>
        )}
        <span className="vp-time">{fmt(time)} | {fmt(duration)}</span>
        <div className="vp-menu-wrap" onMouseEnter={() => setMenu('volume')}>
          <button className="vp-btn" onClick={toggleMute} title={muted ? 'Unmute' : 'Mute'}>{muted || volume === 0 ? '🔇' : '🔊'}</button>
          {menu === 'volume' && <div className="vp-menu volume"><input type="range" min="0" max="1" step="0.05" value={muted ? 0 : volume} onChange={(e) => changeVolume(Number(e.target.value))} /></div>}
        </div>
        <button className="vp-btn" onClick={fullscreen} title="Fullscreen">⛶</button>
      </div>
    </div>
  )
}
