# [QST-03] Gateway не маршрутизирует /api/questions/{id} → 404 на все запросы

**Labels:** bug, api-gateway, severity: high
**ID проверок:** QST-03 (все 17 запросов папки), в т.ч. QST-03 N1–N3

## Шаги
1. Залогиниться instructor, создать вопрос в своём курсе (QST-02) → получить `questionId`.
2. `GET http://localhost:8080/api/questions/{questionId}` с `Authorization: Bearer {{instructorToken}}`.
3. То же самое без заголовка `Authorization`.

## Ожидаемо
- Шаг 2: `200`, вопрос с `data` (как при прямом вызове `GET http://localhost:8082/api/questions/{id}` с токеном admin — он возвращает 200).
- Шаг 3: `401`.

## Фактически
- Шаг 2: `404` (тело gateway, `Content-Type: application/json`, не problem+json).
- Шаг 3: `404` вместо `401` — запрос даже не доходит до проверки токена.
- Аналогично `PUT` и `DELETE /api/questions/{id}`: редактировать и удалять вопросы из банка через приложение невозможно.

## Причина
`services/api-gateway/src/main/resources/application.yml`, маршрут `course-service`:
`Path=/api/courses/**,/api/categories/**,/api/enrollments/**,/api/units/**,/api/certificates/**` — нет `/api/questions/**`.
Правила доступа в `JwtAuthFilter` (`path.startsWith("/api/questions")`) уже есть, не хватает только маршрута.

## Окружение
Прогон Postman Runner 23.10.2026, коллекция `docs/postman/ainura/JazzLMS_API.postman_collection.json`.
