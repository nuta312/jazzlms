/**
 * Единственное место, где фронт ходит в сеть.
 * Все запросы идут на /api/** -> api-gateway -> нужный микросервис.
 * JWT хранится в localStorage и добавляется в заголовок Authorization.
 */
const TOKEN_KEY = 'jazzlms.token'
const USER_KEY = 'jazzlms.user'
const MODE_KEY = 'jazzlms.mode'
const ORIGINAL_KEY = 'jazzlms.original'

export const auth = {
  token: () => localStorage.getItem(TOKEN_KEY),
  user: () => JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
  save: ({ token, user }) => {
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(MODE_KEY)
    localStorage.removeItem(ORIGINAL_KEY)
  },
}

/**
 * "Log into account": админ получает токен другого пользователя.
 * Свою сессию прячем в ORIGINAL_KEY, чтобы вернуться одной кнопкой, не вводя пароль заново.
 */
export const impersonation = {
  start: (res) => {
    localStorage.setItem(ORIGINAL_KEY, JSON.stringify({ token: auth.token(), user: auth.user() }))
    localStorage.removeItem(MODE_KEY)
    auth.save(res)
  },
  original: () => JSON.parse(localStorage.getItem(ORIGINAL_KEY) || 'null'),
  stop: () => {
    const original = impersonation.original()
    if (!original) return
    localStorage.removeItem(ORIGINAL_KEY)
    localStorage.removeItem(MODE_KEY)
    auth.save(original)
  },
}

/** Скачать файл с защищённого эндпоинта: обычная ссылка <a href> не отправит заголовок Authorization. */
export async function download(path, filename) {
  const res = await fetch(path, { headers: { Authorization: `Bearer ${auth.token()}` } })
  if (!res.ok) throw new ApiError(res.status, `HTTP ${res.status}`)
  const url = URL.createObjectURL(await res.blob())
  Object.assign(document.createElement('a'), { href: url, download: filename }).click()
  URL.revokeObjectURL(url)
}

/**
 * Режим интерфейса, как переключатель Administrator / Instructor / Learner в TalentLMS.
 * Это только ВИД: права всё равно проверяет бэкенд по userType из JWT.
 * Админ может посмотреть портал глазами преподавателя и ученика, преподаватель — глазами ученика.
 */
const MODES_BY_TYPE = {
  SUPER_ADMIN: ['admin', 'instructor', 'learner'],
  ADMIN: ['admin', 'instructor', 'learner'],
  TRAINER: ['instructor', 'learner'],
  LEARNER: ['learner'],
}
export const MODE_LABEL = { admin: 'Administrator', instructor: 'Instructor', learner: 'Learner' }

export const mode = {
  available: () => MODES_BY_TYPE[auth.user()?.userType] || ['learner'],
  current: () => {
    const allowed = mode.available()
    const saved = localStorage.getItem(MODE_KEY)
    return allowed.includes(saved) ? saved : allowed[0]
  },
  set: (m) => localStorage.setItem(MODE_KEY, m),
  /** может ли пользователь управлять контентом в текущем режиме */
  canManage: () => mode.current() !== 'learner',
  isAdmin: () => mode.current() === 'admin',
}

export class ApiError extends Error {
  constructor(status, detail) {
    super(detail || `HTTP ${status}`)
    this.status = status
  }
}

async function request(method, path, body) {
  const headers = { 'Content-Type': 'application/json' }
  const token = auth.token()
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(path, { method, headers, body: body ? JSON.stringify(body) : undefined })

  if (res.status === 401 && !path.startsWith('/api/auth/login')) {
    auth.clear()
    window.location.href = '/login'
    return
  }
  if (res.status === 204) return null
  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) throw new ApiError(res.status, data?.detail || data?.message)
  return data
}

/**
 * Загрузка файла (multipart/form-data) с индикатором прогресса.
 * fetch не умеет сообщать прогресс отправки, поэтому здесь XMLHttpRequest.
 * Content-Type не задаём: браузер сам поставит multipart с правильным boundary.
 */
export function upload(path, formData, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', path)
    xhr.setRequestHeader('Authorization', `Bearer ${auth.token()}`)
    xhr.upload.onprogress = (e) => e.lengthComputable && onProgress?.(Math.round((e.loaded / e.total) * 100))
    xhr.onload = () => {
      let data = null
      try { data = xhr.responseText ? JSON.parse(xhr.responseText) : null } catch { /* не JSON */ }
      if (xhr.status >= 200 && xhr.status < 300) resolve(data)
      else reject(new ApiError(xhr.status, data?.detail || (xhr.status === 413 ? 'File is too large' : `HTTP ${xhr.status}`)))
    }
    xhr.onerror = () => reject(new ApiError(0, 'Network error'))
    xhr.send(formData)
  })
}

export const api = {
  get: (p) => request('GET', p),
  post: (p, b) => request('POST', p, b),
  put: (p, b) => request('PUT', p, b),
  patch: (p, b) => request('PATCH', p, b),
  delete: (p) => request('DELETE', p),
}
