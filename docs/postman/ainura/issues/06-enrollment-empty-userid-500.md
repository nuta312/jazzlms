# [ENR-02] Запись на курс с пустым userId → 500 вместо 400

**Labels:** bug, course-service, severity: low
**ID проверки:** найдено в подготовке зоны Tests (запрос «Подготовка · POST /api/courses/{id}/enrollments»), относится к ENR-02 (зона Zhyldyz)

## Шаги
`POST http://localhost:8080/api/courses/{courseId}/enrollments` (instructor) с телом `{"userId": ""}`.

## Ожидаемо
`400`, problem+json, `detail: "userId: must not be null"` (или «invalid UUID»).

## Фактически
`500`, `Content-Type: application/problem+json`, `detail: "Internal server error, see service logs"`.

## Причина
Пустая строка не десериализуется в UUID → `HttpMessageNotReadableException` → общий обработчик 500 (та же корневая причина, что в issue про «тело не JSON»).
