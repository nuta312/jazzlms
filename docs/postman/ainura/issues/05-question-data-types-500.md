# [QST-02 Extra] Неверные типы в data вопроса → 500 вместо 400

**Labels:** bug, course-service, severity: low
**ID проверок:** Extra · RANDOMIZED pool с не-UUID строкой; Extra · FREE_TEXT threshold строкой

## Шаги
1. `POST /api/courses/{courseId}/questions` с телом
   `{"type": "RANDOMIZED", "text": "Bad pool", "data": {"pool": ["abc"]}}`
2. `POST /api/courses/{courseId}/questions` с телом
   `{"type": "FREE_TEXT", "text": "Bad threshold", "data": {"options": [{"word": "a"}], "threshold": "two"}}`

## Ожидаемо
`400` с понятным `detail` («pool must contain question ids», «threshold must be a number»).

## Фактически
`500`, `detail: "Internal server error, see service logs"`.

## Причина
`TestService.applyQuestion`: `UUID.fromString(id)` без проверки (IllegalArgumentException) и `((Number) d.get("threshold"))` без проверки типа (ClassCastException).
