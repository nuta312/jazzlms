# Alina · JazzLMS API tests · Branches & Groups

Домашка по чек-листу `API_TEST_CHECKLIST.md` — зона **Alina**:
`user-service` → **Branches, Groups, membership** (§3.3, 18 эндпоинтов) + **E2E-3** (Default group и политика паролей).

## Файлы

| Файл | Что это |
|---|---|
| [JazzLMS-Alina.postman_collection.json](JazzLMS-Alina.postman_collection.json) | Postman-коллекция (v2.1), **96 запросов** |
| [JazzLMS-local.postman_environment.json](JazzLMS-local.postman_environment.json) | Окружение `JazzLMS local`, значения пустые (§8.1) |
| [build_collection.py](build_collection.py) | Генератор коллекции (можно пересобирать) |

## Структура коллекции

```
JazzLMS · Alina · Branches & Groups
├── 00 Setup                                              (4)
│   ├── SETUP-01 · login admin         → adminToken, adminId
│   ├── SETUP-02 · login instructor    → instructorToken, instructorId
│   ├── SETUP-03 · POST /api/users     → learner_alina_<ts>, learnerId
│   └── SETUP-04 · login learner       → learnerToken
├── ORG · Branches (Alina)                                (38)
├── ORG · Groups (Alina)                                  (38)
├── 07 E2E · E2E-3 Default group & password policy        (15)
└── 99 Teardown                                           (1)
```

Внутри каждой папки `ORG · …` закрыты все 9 эндпоинтов из §3.3 для своей сущности:
`ORG-01` список · `ORG-02` создание · `ORG-03` чтение по id · `ORG-04` PUT · `ORG-05` DELETE ·
`ORG-06` список участников · `ORG-07` добавить участника · `ORG-08` убрать участника · `ORG-09`
`GET /api/users/{id}/{X}`.

Итого **18 эндпоинтов × ≥1 позитив × ≥3 негатива** из §1.4 (чаще всего — N1 без токена, N2 мусорный
токен, N4 роль ниже, N5 несуществующий UUID, N6 невалидный UUID). «Зафиксировать» помечено прямо в
имени запроса — `ORG-02 duplicate name · 409 or 201 (fix)`, `ORG-07 add · 201/204`.

## Запуск

### Коллекция «с нуля»

```bash
docker compose --profile app down -v && docker compose --profile app up -d --build
```

### Через Newman

```bash
cd /path/to/this/folder
npm i -g newman newman-reporter-htmlextra   # один раз
newman run JazzLMS-Alina.postman_collection.json \
       -e JazzLMS-local.postman_environment.json \
       --reporters cli,htmlextra
```

### Через Postman CLI (установлен в `/Users/alina/.local/bin/postman`)

```bash
/Users/alina/.local/bin/postman collection run JazzLMS-Alina.postman_collection.json \
       --environment JazzLMS-local.postman_environment.json
```

### Через Collection Runner в Postman UI

Импортировать оба файла → выбрать окружение `JazzLMS local` → Runner → запустить коллекцию целиком
в порядке папок.

## Что проверяет каждый запрос (§1.3)

Все проверки лежат во вкладке **Tests** каждого запроса. Общие блоки:

**Позитивный:**
- код ответа соответствует ожидаемому;
- `Content-Type` содержит `application/json`;
- время ответа < 1500 мс;
- обязательные поля, типы (`id` — UUID, `createdAt` — ISO);
- нет лишнего (`passwordHash` явно проверяется в `ORG-06`);
- `id`-поля сохраняются в окружение.

**Негативный:**
- код ответа соответствует ожидаемому;
- `Content-Type: application/problem+json`;
- `title`, `status` (число, совпадает с кодом), `detail` (непустая строка);
- `detail` не содержит `.java:`, `Exception`, стек-трейсов.

## E2E-3

Полностью соответствует шагам чек-листа:

1. создать группу «Newcomers»;
2. сохранить текущие settings → поставить `defaultGroupId` и `passwordChangeOnFirstLogin = true`;
3. создать ученика (должен попасть в default-группу);
4. `GET /api/groups/{id}/users` — ученик там;
5. login — `mustChangePassword = true`;
6. сменить пароль → login снова → `mustChangePassword = false`;
7. включить `lockAfterAttempts = 2, lockMinutes = 1` → 2 неверных → 3-й даёт **423**;
8. teardown: вернуть settings к дефолтам (§1.4: `lockAfterAttempts = 3, lockMinutes = 30, signupMode = DIRECT, termsOfService = null, defaultGroupId = null`), удалить ученика и группу.

Шаг «redis-cli TTL login:fail» в коллекции не покрыт — его стоит проверить руками
(`docker exec jazzlms-redis-1 redis-cli TTL login:fail:<username>` ≤ 60) и приложить скрин к PR.

## Definition of Done (§1.6)

- [x] Все 18 эндпоинтов зоны покрыты.
- [x] У каждого запроса — позитив + минимум 3 негатива из §1.4 плюс специфичные.
- [x] Вкладка Tests с проверками кода, заголовка и тела.
- [x] Переменные пробрасываются скриптами (ничего руками).
- [x] Teardown удаляет созданное (`99 Teardown` + Teardown внутри E2E-3).
- [ ] Папка проходит в Runner **зелёной 3 раза подряд** — проверить на поднятом окружении.
- [ ] Найденные дефекты (кандидаты из «fix» в именах запросов) завести в GitHub Issues с ID проверки.

## Как сдавать (§8)

1. Положить файлы в `docs/postman/`:
   - `JazzLMS.postman_collection.json` (общая коллекция команды, куда подключится папка Alina)
     или отдельная `JazzLMS-Alina.postman_collection.json`.
   - `JazzLMS-local.postman_environment.json` — значения переменных пустые (без токенов). ✅
2. PR с описанием: 96 запросов, по 5-9 проверок каждый, список кандидатов в дефекты (имена с
   пометкой `(fix)` и теми, где по чек-листу написано «зафиксировать»).
3. Kantemir добавит папку в общий Newman-прогон.
