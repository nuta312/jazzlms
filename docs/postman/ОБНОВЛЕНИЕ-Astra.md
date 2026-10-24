# Обновление каркаса JazzLMS (от Astra) — что изменилось и что делать

Файлы в `docs/postman/`:
- `JazzLMS.postman_collection.json` — коллекция: Setup, Auth, Demo, E2E-1, Teardown;
- `JazzLMS-local.postman_environment.json` — окружение (значения токенов пустые);
- `BUGS-auth.md`, `BUG-500-malformed-body.md` — дефекты зоны Auth (заведены в Issues).

Прогон Runner, 3 итерации подряд: **750 проверок, 0 Failed, 0 Errors**, 9 skipped (известные баги, см. ниже).

---

## ⚠️ Важно всем: токены больше НЕ secret

`adminToken`, `instructorToken`, `learnerToken` в окружении были типа **secret**. В Runner скрипты
(`pm.environment.get("adminToken")` внутри `pm.sendRequest`) читали у secret-переменных старое сохранённое
значение, а не свежий токен из Setup → запросы из скриптов получали **401**, а иногда и обычные запросы
(так падал SETUP-03).

**Что сделать:** в окружении **JazzLMS local** у трёх токенов нажать на замок → **Not a secret** → Cmd + S.
(Или импортировать новое окружение с Replace — в нём они уже обычные.) Значения токенов в файле пустые,
в git они не попадают.

---

## Новые переменные окружения

| Переменная | Кто заполняет | Зачем |
|---|---|---|
| `strictGatewayErrors` | вы руками | общий переключатель команды (как у Ainura): `false` — проверки формата ответов gateway (#2) пропускаются, коды проверяются |
| `learnerEmail` | SETUP-03 | email вашего ученика из ответа логина (у старых учеников он мог отличаться от шаблона) |

---

## Что изменилось в Setup / Teardown

- **SETUP-03**: при 409 (ученик уже есть) `learnerId` и `learnerEmail` теперь берутся из ответа логина —
  раньше `learnerId` оставался пустым.
- **TD-01** удаляет ученика и всех пользователей, созданных запросами Auth и E2E-1 (на следующем прогоне
  Setup создаёт ученика заново — так задумано).
- **TD-02** возвращает Account Settings из снимка, если какой-то тест упал посередине.

Тесты, которые меняют Settings (блокировка, signup MANUAL, Terms of Service, Default user type,
password change on first login), сами берут снимок настроек и сами его восстанавливают.
Если вы тоже меняете Settings — пользуйтесь той же переменной `settingsBackup`, тогда TD-02 подстрахует и вас.

---

## Если вы ЕЩЁ НЕ начинали

1. Postman → **Import** → оба файла.
2. Справа вверху выберите окружение **JazzLMS local**.
3. Поставьте свой `learnerUsername` = `learner_<ваше_имя>` (по умолчанию `learner_astra`) и `strictGatewayErrors` = `false`.
4. Поднимите платформу: `cd ~/IdeaProjects/jazzlms && docker compose --profile app up -d`.
5. Запустите папку **00 Setup** — токены заполнятся.

## Если вы УЖЕ работали

1. Окружение: снимите secret с трёх токенов (см. выше) и добавьте `strictGatewayErrors` = `false`.
   Или Import окружения → **Replace**, потом снова свой `learnerUsername` и `strictGatewayErrors`.
2. **Коллекцию целиком не импортируйте** — потеряете свои папки. Общую версию собирает Kantemir.

---

## Известные дефекты (проверки помечены skipped, прогон зелёный)

| Issue | Что | Где в коллекции |
|---|---|---|
| [#10](https://github.com/nuta312/jazzlms/issues/10) | нечитаемое тело / нет Content-Type → 500 | AUTH-01 · без Content-Type (`KNOWN_BUG`) |
| [#3](https://github.com/nuta312/jazzlms/issues/3) | невалидный UUID в пути → 500 | AUTH-05 · невалидный UUID (`KNOWN_BUG`) |
| [#2](https://github.com/nuta312/jazzlms/issues/2) | gateway 401/403 без тела и `WWW-Authenticate` | AUTH-06 · N1 (`strictGatewayErrors`) |

Когда дефект починят — в скрипте поставить `KNOWN_BUG = false` (или `strictGatewayErrors = true`).

---

## Отдельно для Kantemir

Пересобери, пожалуйста, общую коллекцию своим скриптом: обновились **00 Setup**, **01 user-service → Auth**,
новые папки **01 user-service → Demo**, **07 E2E → E2E-1**, **99 Teardown** (TD-01, TD-02) и окружение.
