# Postman: зона Ainura (Tests) — QST-01…04, TST-01…07, E2E-6

## Файлы
- `JazzLMS-Tests-Ainura.postman_collection.json` — коллекция, 217 запросов.
- `JazzLMS-local.postman_environment.json` — окружение (токены пустые, заполняются скриптами).

## Как запустить
1. `docker compose --profile app down -v && docker compose --profile app up -d --build`
2. Postman → Import → оба файла → выбрать окружение `JazzLMS local` → Run collection.
   Или: `newman run JazzLMS-Tests-Ainura.postman_collection.json -e JazzLMS-local.postman_environment.json`

## Структура
| Папка | Что делает |
|---|---|
| 00 Setup · Ainura | логины admin/instructor; создаёт `learner_ainura`, `learner_ainura2` (не записан), `instructor_ainura2` (чужой преподаватель); курс, чужой курс, запись ученика, обычный урок CONTENT |
| 02 course-service · Tests | QST-02 → QST-01 → QST-04 → TST-01 → TST-02 → TST-03 → TST-04/05 → TST-06/07 → QST-03 → Extra |
| 07 E2E · E2E-6 | провал, пересдача, IF_NOT_PASSED, отчёт, очки, Timeline |
| 99 Teardown · Ainura | удаляет курсы (мягко + permanent) и созданных пользователей |

Порядок внутри папок важен: попытки опираются на созданные вопросы и тесты.

## Как устроены скрипты
- Общие функции лежат в pre-request каждой верхней папки и подключаются строкой `eval(pm.collectionVariables.get('tstLib'))`.
- `T.ok(код)` проверяет код, `Content-Type`, время < 1500 мс. `T.fail(код, 'текст')` проверяет код и формат problem+json (§1.3).
- Асинхронные события (Timeline, геймификация) проверяются опросом каждые 500 мс до 10 с.
- Переменные зоны начинаются с `tst_` (E2E — с `e6_`), чтобы не затирать `courseId` и т. п. других студентов.
- Если Setup упал на полпути, повторный прогон переиспользует существующих пользователей (409 → поиск → сброс пароля).

## Дефекты (подтверждены прогоном 23.10: 1104 проверки, 975 ✓, 129 ✗ — все ✗ ниже)
| ID проверки | Что | Причина |
|---|---|---|
| QST-03 (все) | `/api/questions/{id}` через gateway → 404 | в `api-gateway/application.yml` маршрут course-service не содержит `/api/questions/**`; прямой запрос на :8082 работает (запрос «direct» в папке QST-03) |
| N6 (QST-01, QST-02, TST-02, TST-05, TST-07, QST-03) | невалидный UUID `abc` → 500 вместо 400 | `GlobalExceptionHandler` не обрабатывает `MethodArgumentTypeMismatchException` |
| QST-02 N8, `type = ESSAY` | тело не JSON / неизвестный тип → 500 вместо 400 | не обрабатывается `HttpMessageNotReadableException` |
| N1–N3, N4 через gateway | 401/403 без тела, нет `application/problem+json` и `WWW-Authenticate` | `JwtAuthFilter.reject` ставит только код. Пока не исправлено, можно поставить `strictGatewayErrors = false` — проверки формата будут пропущены, коды проверяются всё равно |
| Extra | pool с не-UUID, `threshold: "two"` → 500 | приведения типов без проверки в `TestService` (`UUID.fromString`, `(Number)`) |
| QST-03 N1–N3 | без токена / с мусорным токеном `/api/questions/{id}` → 404, а не 401 | следствие того же отсутствующего маршрута в gateway |
| (подготовка) | `POST /api/courses/{id}/enrollments` с пустым `userId` → 500 | пустая строка вместо UUID не обрабатывается как 400 |

## Отличия от чек-листа (уточнено по коду)
- FREE_TEXT: варианты задаются как `{ "word": "...", "mode": "contains" | "equals" | "not_contains", "points": 1 }`, а не полем `contains`.
- Импорт AIKEN отвечает 200 (не 201).
- Ответы при submit: MC — массив индексов `[0]`, FILL_GAP — массив строк, ORDERING — id в правильном порядке `[0,1,2]`, DRAG_DROP — `{"0":0,"1":1}`, FREE_TEXT — строка.
- В ответе submit `results[].correct` — это отметка «верно/неверно», а не правильный ответ; проверка «нет `correct`» относится только к старту попытки.
