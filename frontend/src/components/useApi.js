import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'

/** Маленький хук: загрузить данные по URL, дать reload() и состояние ошибки/загрузки.
 *  Если path изменился (другой period, tab, search) — данные перезагружаются автоматически. */
export function useApi(path) {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  const reload = useCallback(() => {
    if (!path) return                     // path === null -> ничего не грузим (условный запрос)
    setLoading(true)
    api.get(path).then(setData).catch((e) => setError(e.message)).finally(() => setLoading(false))
  }, [path])

  useEffect(() => { reload() }, [reload])   // path поменялся -> reload пересоздан -> перезапрос
  return { data, error, loading, reload, setData }
}

export const fmtDate = (iso) => (iso ? new Date(iso).toLocaleDateString('en-GB') : '-')
export const fmtDateTime = (iso) => (iso ? new Date(iso).toLocaleString('en-GB', { dateStyle: 'short', timeStyle: 'short' }) : '-')
