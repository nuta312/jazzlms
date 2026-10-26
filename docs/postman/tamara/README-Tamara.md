# JazzLMS — зона Users и Settings (Tamara)

## Что добавлено
- JazzLMS-Users-Tamara.postman_collection.json — запросы USR-01..USR-08, SET-01, SET-02
- JazzLMS_local-Tamara.postman_environment.json — окружение без токенов

## Как запустить
1. Поднять JazzLMS: `docker compose --profile app up -d`
2. Импортировать коллекцию и окружение в Postman
3. Выбрать окружение JazzLMS local
4. Запустить папку через Collection Runner

## Скрипты
- Post-response скрипты автоматически сохраняют токен
- Bearer Token наследуется на уровне папки Users

## Найденные дефекты
- #8 — пустой email при PUT сохраняется с 200 вместо 400
- #9 — неверный userType возвращает 500 вместо 400
- #3 — неверный UUID возвращает 500 вместо 400 (комментарий)
