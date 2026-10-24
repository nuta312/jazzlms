# [QST-02] Тело не JSON или неизвестный тип вопроса → 500 вместо 400

**Labels:** bug, course-service, severity: medium
**ID проверок:** QST-02 N8, QST-02 «type = ESSAY»

## Шаги
1. `POST http://localhost:8080/api/courses/{courseId}/questions`, `Content-Type: application/json`, тело `this is not json`.
2. Тот же запрос с телом `{"type": "ESSAY", "text": "Essay", "data": {}}`.

## Ожидаемо
`400`, problem+json, `detail` о некорректном теле / недопустимом значении `type` (допустимые: MULTIPLE_CHOICE, FILL_GAP, ORDERING, DRAG_DROP, FREE_TEXT, RANDOMIZED).

## Фактически
`500`, `detail: "Internal server error, see service logs"`.

## Причина
Не обрабатывается `HttpMessageNotReadableException` в `GlobalExceptionHandler`.
Тот же дефект Astra поймала на логине: табуляция внутри JSON-строки → 500 (связано, можно закрыть одним исправлением).
