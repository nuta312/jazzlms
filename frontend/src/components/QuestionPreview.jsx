export const Q_ICON = { MULTIPLE_CHOICE: '☑', FILL_GAP: '▭', ORDERING: '↕', DRAG_DROP: '⇄', FREE_TEXT: '✎', RANDOMIZED: '🎲' }
export const Q_LABEL = { MULTIPLE_CHOICE: 'Multiple choice', FILL_GAP: 'Fill the gap', ORDERING: 'Ordering', DRAG_DROP: 'Drag-and-drop', FREE_TEXT: 'Free text', RANDOMIZED: 'Randomized' }

/** Предпросмотр вопроса для преподавателя: показывает и правильные ответы. */
export default function QuestionPreview({ question: q, onClose }) {
  const d = q.data || {}
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head"><h2>{Q_ICON[q.type]} {Q_LABEL[q.type]}</h2><button className="link-btn" onClick={onClose}>✕</button></div>
        <p className="q-text">{q.text}</p>
        {q.type === 'MULTIPLE_CHOICE' && (d.answers || []).map((a, i) => <div key={i} className={`q-opt ${a.correct ? 'ok' : ''}`}>{a.correct ? '✔' : '○'} {a.text}</div>)}
        {q.type === 'FILL_GAP' && <p className="hint">Gaps are in [brackets]; the first alternative is the correct one.</p>}
        {q.type === 'ORDERING' && <ol>{(d.items || []).map((x, i) => <li key={i}>{x}</li>)}</ol>}
        {q.type === 'DRAG_DROP' && (d.pairs || []).map((p, i) => <div key={i} className="q-opt">{p.left} <b>→</b> {p.right}</div>)}
        {q.type === 'FREE_TEXT' && <>{(d.options || []).map((o, i) => <div key={i} className="q-opt">{o.mode} "{o.word}" → +{o.points} points</div>)}<p className="hint">Correct when points ≥ {d.threshold}</p></>}
        {q.type === 'RANDOMIZED' && <p className="hint">Pool of {(d.pool || []).length} question(s); one is picked at random for each attempt.</p>}
        {q.feedback && <p className="hint">Feedback: {q.feedback}</p>}
      </div>
    </div>
  )
}
