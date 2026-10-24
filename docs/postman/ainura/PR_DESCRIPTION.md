# Postman: зона Tests (Ainura) — QST-01…04, TST-01…07, E2E-6

**Ветка:** `ainura/postman-tests` → `master`

## Что добавлено
`docs/postman/ainura/`:
- `JazzLMS_API.postman_collection.json` — общий каркас (Astra) + папки зоны Tests:
  - `02 course-service → Tests (Ainura)` — подготовка, QST-01…04, TST-01…07, Extra;
  - `07 E2E → E2E-6 · Тест: провал, пересдача, очки`;
  - `99 Teardown → Ainura · Tests`.
- `JazzLMS_local.postman_environment.json` — окружение без токенов (значения токенов пустые).
- `JazzLMS-Tests-Ainura.postman_collection.json` — та же зона отдельной коллекцией (запускается без общего Setup).
- `README-Ainura.md` — запуск, устройство скриптов, отличия от чек-листа.
- `issues/` — тексты заведённых дефектов.

## Цифры
| | |
|---|---|
| Запросов в зоне | 217 |
| Проверок (pm.test) в прогоне | 1082 |
| Прогон 23.10.2026 (вся коллекция) | 1104 проверки: 975 ✓ / 129 ✗ |
| Красные | все 129 — дефекты ниже (+1 у SETUP-03 Astra, 409 при повторном прогоне) |

Позитивные сценарии зоны проходят полностью: 6 типов вопросов, импорт AIKEN, создание/редактирование тестов,
попытки, подсчёт баллов по каждому типу, веса, лимиты попыток (NEVER / maxAttempts / IF_NOT_PASSED), shuffle,
sequential, E2E-6 (очки «Test passed +20» ровно один раз, 2 события TEST_COMPLETED в Timeline).

## Найденные дефекты
| # | ID проверки | Дефект |
|---|---|---|
| 1 | QST-03 | Gateway не маршрутизирует `/api/questions/{id}` → 404 (даже без токена) |
| 2 | N1–N4 | Отказы gateway 401/403 без тела, без problem+json и `WWW-Authenticate` |
| 3 | N6 (QST-02, TST-02, TST-05, TST-07) | Невалидный UUID в пути → 500 вместо 400 |
| 4 | QST-02 N8, `type = ESSAY` | Тело не JSON / неизвестный enum → 500 вместо 400 |
| 5 | Extra | `pool: ["abc"]`, `threshold: "two"` → 500 вместо 400 |
| 6 | ENR-02 (подготовка) | `userId: ""` при записи на курс → 500 вместо 400 |

Issues: #… #… #… #… #… #…  ← вставить номера после создания

## Как проверить
```
docker compose --profile app down -v && docker compose --profile app up -d --build
cd docs/postman/ainura
npx newman run JazzLMS_API.postman_collection.json -e JazzLMS_local.postman_environment.json
```

## Зафиксировано (обсудить)
- Импорт AIKEN отвечает `200`, а не `201`.
- Ответ MULTIPLE_CHOICE строкой (`"0"` вместо `[0]`) → `200`, засчитывается как неверный, ошибки нет.
- `SETUP-03` (Astra): при повторном прогоне 409, `learnerToken` не обновляется — моя подготовка логинит ученика сама.
