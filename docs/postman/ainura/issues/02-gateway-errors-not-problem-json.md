# [N1–N4] Отказы gateway 401/403 приходят с пустым телом, без problem+json и WWW-Authenticate

**Labels:** bug, api-gateway, severity: medium
**ID проверок:** N1, N2, N3, N4 в QST-01, QST-02, QST-04, TST-01, TST-02, TST-03, TST-04, TST-06 (≈ 60 проверок)

## Шаги
1. `GET http://localhost:8080/api/courses/{courseId}/questions` без заголовка `Authorization`.
2. То же с `Authorization: Bearer abc.def.ghi`.
3. То же с токеном learner.

## Ожидаемо (чек-лист §1.3, §1.4)
- Шаги 1–2: `401`, `Content-Type: application/problem+json`, тело `{title, status: 401, detail}`, заголовок `WWW-Authenticate: Bearer`.
- Шаг 3: `403`, `application/problem+json`, `{title, status: 403, detail}`.

## Фактически
Код верный (401 / 403), но тело пустое, `Content-Type` отсутствует, `WWW-Authenticate` нет.
Ошибки самих сервисов при этом отдаются в problem+json — формат ответов API неоднородный.

## Причина
`JwtAuthFilter.reject()` в api-gateway только ставит статус и вызывает `setComplete()`, тело не пишет.

## Примечание для прогонов
Пока не исправлено, в окружении можно поставить `strictGatewayErrors = false` — проверки формата будут пропущены, коды проверяются.
