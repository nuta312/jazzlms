# Дефекты зоны Auth (Astra)

Заведены в https://github.com/nuta312/jazzlms/issues. Мои #11 и #12 закрыты как дубли #3 и #2.
#3: в коллекции флаг `KNOWN_BUG = true` в скрипте (skipped), после исправления поставить `false`.
#2: общий переключатель окружения `strictGatewayErrors` (как у Ainura): `false` — проверка формата gateway пропускается.

---

## 1. Невалидный UUID в пути → 500 вместо 400 — [#3](https://github.com/nuta312/jazzlms/issues/3)

**Проверка:** AUTH-05 · POST /api/auth/impersonate/abc (N6 из §1.4)
**Шаги:** `POST /api/auth/impersonate/abc` токеном admin.
**Ожидание:** 400 Bad Request.
**Факт:** 500 `{"title":"Internal Server Error","detail":"Internal server error, see service logs"}`.
**Причина (предположительно):** `GlobalExceptionHandler` не ловит `MethodArgumentTypeMismatchException`.
Скорее всего, так же ведут себя все эндпоинты с `{id}` в user-service. Похоже на #10 (нечитаемое тело → 500): можно чинить вместе.
**Критичность:** Medium.

---

## 2. Gateway на 401 не отдаёт `WWW-Authenticate: Bearer` — [#2](https://github.com/nuta312/jazzlms/issues/2)

**Проверка:** AUTH-06 · GET /api/auth/me · N1 без токена → 401
**Шаги:** любой защищённый запрос через gateway без заголовка `Authorization`.
**Ожидание (§1.4 N1, RFC 6750):** 401 и заголовок `WWW-Authenticate: Bearer`.
**Факт:** 401, пустое тело, заголовка нет. Ответы gateway на 401/403 также не в формате problem+json.
**Где:** `api-gateway` → `JwtAuthFilter.reject()`.
**Критичность:** Low.
