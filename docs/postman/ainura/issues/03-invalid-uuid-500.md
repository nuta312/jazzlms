# [N6] Невалидный UUID в пути → 500 вместо 400

**Labels:** bug, course-service, severity: medium
**ID проверок:** QST-02 N6, TST-02 N6, TST-05 N6, TST-07 N6

## Шаги
1. `POST http://localhost:8080/api/courses/abc/questions` (instructor, валидное тело).
2. `GET http://localhost:8080/api/units/abc/test` (instructor).
3. `POST http://localhost:8080/api/units/{unitId}/test/attempts/abc/submit` (learner).
4. `GET http://localhost:8080/api/units/{unitId}/test/attempts/abc` (instructor).

## Ожидаемо
`400`, `application/problem+json`, `detail` с указанием параметра (например «Invalid UUID: abc»).

## Фактически
`500`, `detail: "Internal server error, see service logs"`.

## Причина
`GlobalExceptionHandler` course-service не обрабатывает `MethodArgumentTypeMismatchException`, она попадает в общий `@ExceptionHandler(Exception.class)` → 500.
