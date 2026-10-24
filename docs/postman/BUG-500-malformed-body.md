# Issue [#10](https://github.com/nuta312/jazzlms/issues/10) (Astra)

Заведён: https://github.com/nuta312/jazzlms/issues/10.
В коллекции: AUTH-01 · без Content-Type → 415/400 (`KNOWN_BUG = true`)

---

## Title
POST-эндпоинты возвращают 500 вместо 400/415 при некорректном теле запроса

## Body

## Описание
Несколько POST-эндпоинтов отвечают `500 Internal Server Error` на запрос с некорректным/непрочитываемым телом, хотя это ошибка клиента и ожидается `400 Bad Request` (или `415` при отсутствии Content-Type). Ошибки парсинга тела (`HttpMessageNotReadableException`) не обрабатываются в `GlobalExceptionHandler`.

## Окружение
- JazzLMS, локально через Docker (`docker compose --profile app up -d`)
- gateway: http://localhost:8080
- user-service (воспроизведено на `POST /api/users`)

## Сценарий 1 — нет заголовка Content-Type
**Шаги:** `POST /api/users` с валидным JSON-телом, но без `Content-Type: application/json`.
**Ожидание:** 400 или 415.
**Факт:** 500.
```json
{"title":"Internal Server Error","detail":"Internal server error, see service logs","status":500}
```

## Сценарий 2 — управляющий символ (таб) в теле
**Шаги:** `POST /api/users` с `Content-Type: application/json`, но в значении поля есть символ табуляции (code 9).
**Ожидание:** 400 Bad Request.
**Факт:** 500. В логах user-service:
```
HttpMessageNotReadableException: JSON parse error: Illegal unquoted character (CTRL-CHAR, code 9)
```

## Причина (предположительно)
`GlobalExceptionHandler` не ловит `HttpMessageNotReadableException` / `HttpMediaTypeNotSupportedException` и отдаёт дефолтный 500.

## Где ещё воспроизводится
По словам Kantemir, та же картина в notification-service и gamification-service (любое нечитаемое тело → 500).

## Проверки, где поймано
SETUP-03 / USR-03 (Postman-коллекция, зона Auth/Users).

## Критичность
Medium — ломает корректную обработку ошибок клиента, маскирует 400/415 под 500.
