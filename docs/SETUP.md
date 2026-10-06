# Как развернуть JazzLMS у себя (инструкция для студентов)

Цель: за 15–20 минут поднять на своём компьютере всю систему — 6 Java-сервисов, фронтенд, PostgreSQL,
MongoDB, Redis, Kafka, S3-хранилище (RustFS) и Grafana — и войти в неё как администратор.

## 1. Что установить

| Программа | Версия | Зачем | Проверка в терминале |
|---|---|---|---|
| **Git** | любая | скачать проект | `git --version` |
| **JDK** | **21** (Temurin / Corretto / Oracle) | собрать Java-сервисы | `java -version` → `21...` |
| **Docker Desktop** | свежая | запустить всё в контейнерах | `docker --version` и `docker compose version` |
| Node.js | 20+ | *только* если хотите запускать фронт в dev-режиме | `node -v` |

Gradle ставить **не нужно**: в проекте лежит wrapper (`gradlew`), он сам скачает нужную версию.

**Требования к компьютеру:** 8 ГБ ОЗУ минимум (лучше 16), ~6 ГБ свободного диска.
В Docker Desktop → Settings → Resources выделите Docker не меньше **6 ГБ памяти**.

**Windows:** установите Docker Desktop с WSL 2 (установщик предложит сам). Команды ниже выполняйте
в PowerShell из папки проекта и пишите `.\gradlew.bat` вместо `./gradlew`.

## 2. Скачать проект

```bash
git clone https://github.com/nuta312/jazzlms.git
cd jazzlms
```

## 3. Проверить, что порты свободны

Приложение занимает эти порты на вашем компьютере:

| Порт | Что |
|---|---|
| 3000 | фронтенд (сюда заходим браузером) |
| 8080 | api-gateway |
| 8081–8085, 9091 | микросервисы (REST и gRPC) |
| 5432 | PostgreSQL |
| 27019 | MongoDB |
| 6379 | Redis |
| 19092 | Kafka |
| 8090 | Kafka UI |
| 9002, 9003 | RustFS — S3-хранилище файлов уроков (API и консоль) |
| 3001, 3100, 9090, 9411 | Grafana, Loki, Prometheus, Zipkin |

Чаще всего мешает **локально установленный PostgreSQL на 5432** или другой проект на 8080 — остановите их.
Проверить порт: macOS/Linux `lsof -i :5432`, Windows `netstat -ano | findstr :5432`.

## 4. Собрать и запустить

Docker Desktop должен быть **запущен** (значок кита в трее/меню).

```bash
./gradlew build
```

Собирает jar-файлы всех сервисов и прогоняет тесты. Первый раз — 3–5 минут (скачиваются зависимости).
В конце должно быть `BUILD SUCCESSFUL`.

```bash
docker compose --profile app up -d --build
```

Собирает образы и запускает ~18 контейнеров. Первый раз — 5–10 минут (скачиваются образы).

Сервисам нужно ещё около минуты, чтобы стартовать. Проверка:

```bash
docker compose --profile app ps
```

У всех контейнеров статус `Up` (у баз — `healthy`).

```bash
curl http://localhost:8080/actuator/health
```

Ответ `{"status":"UP"}` — gateway готов.

## 5. Войти

Откройте <http://localhost:3000>.

| Роль | Логин | Пароль |
|---|---|---|
| Администратор | `admin` | `admin123` |
| Преподаватель | `instructor` | `instructor123` |
| Ученик | создайте сами: **Sign up** на странице входа или Users → Add user под админом | |

База у вас будет **пустая**: курсы, уроки, тесты и учеников вы создаёте сами — это и есть практика.
Сценарий «что понажимать» — в разделе «Сценарий для демонстрации» в [README](../README.md).

## 6. Что ещё открыть

| Что | Адрес | Логин |
|---|---|---|
| Swagger (REST API сервиса) | <http://localhost:8081/swagger-ui.html> (также 8082–8085) | — |
| Kafka UI — топики и события | <http://localhost:8090> | — |
| Grafana — логи всех сервисов | <http://localhost:3001> | admin / admin |
| Zipkin — путь запроса по сервисам | <http://localhost:9411> | — |
| RustFS — файлы уроков (бакет `lms-content`) | <http://localhost:9003/rustfs/console/> | jazzlms / jazzlms-secret-key |
| PostgreSQL (pgAdmin / DBeaver) | host `localhost`, port `5432`, базы `users_db` и `courses_db` | lms / lms |
| MongoDB (Compass) | `mongodb://localhost:27019` | — |
| Redis | `docker exec -it jazzlms-redis-1 redis-cli` | — |

## 7. Ежедневная работа

```bash
docker compose --profile app stop
```

Остановить всё, **данные сохранятся**.

```bash
docker compose --profile app start
```

Запустить снова (быстро, без пересборки).

```bash
docker compose --profile app logs -f user-service
```

Смотреть логи одного сервиса (Ctrl+C — выйти).

```bash
docker compose --profile app down -v
```

Удалить всё **вместе с данными** и начать с чистого листа.

**Изменили Java-код?** Пересоберите и перезапустите только этот сервис:

```bash
./gradlew :services:course-service:build -x test
```

```bash
docker compose --profile app up -d --build course-service
```

**Изменили фронтенд?** `docker compose --profile app up -d --build frontend`.

## 8. Режим разработки (сервисы из IntelliJ IDEA)

Удобно для отладки с точками останова: в Docker работает только инфраструктура, сервисы — из IDE.

```bash
docker compose up -d
```

Без `--profile app` поднимаются только базы, Kafka, Redis, RustFS и мониторинг.

Откройте папку проекта в IntelliJ IDEA (она сама импортирует Gradle) и запустите классы `*Application`
каждого сервиса: `user-service` **первым**, затем `course-service`, `notification-service`,
`analytics-service`, `gamification-service`, `api-gateway`. Настраивать ничего не надо: адреса по умолчанию
смотрят на `localhost`.

```bash
cd frontend && npm install && npm run dev
```

Фронт с горячей перезагрузкой: <http://localhost:5173>.

## 9. Если что-то пошло не так

| Симптом | Причина и решение |
|---|---|
| `Cannot connect to the Docker daemon` | Docker Desktop не запущен — запустите и дождитесь, пока кит перестанет «думать» |
| `failed to resolve reference ... 401 UNAUTHORIZED` или `No such image` при `up` | образ не скачался, и compose прервал остальные. Проверьте интернет и повторите `up`. Если ошибка про `quay.io/minio/minio` — у вас старая версия `docker-compose.yml`, выполните `git pull` (MinIO заменён на RustFS) |
| `port is already allocated` / `address already in use` | порт занят другой программой (см. шаг 3). Остановите её и повторите `up` |
| `./gradlew: Permission denied` | `chmod +x gradlew` |
| Сборка падает с `Unsupported class file major version` или `invalid source release: 21` | у вас не JDK 21. Проверьте `java -version`, задайте `JAVA_HOME` на JDK 21 |
| Сайт открывается, но логин даёт ошибку / 502 | сервисы ещё стартуют — подождите минуту. Не помогло: `docker compose --profile app logs user-service api-gateway` |
| Контейнер сервиса постоянно перезапускается | мало памяти у Docker (нужно 6 ГБ+) или упала миграция БД — смотрите логи этого сервиса |
| После `git pull` сервис не стартует, в логах `Flyway ... checksum mismatch` | схема БД изменилась. Проще всего: `docker compose --profile app down -v` и поднять заново |
| Вход заблокирован: `Account is locked` | сработала защита от перебора (3 неверных пароля). Подождите или снимите: `docker exec jazzlms-redis-1 redis-cli DEL login:fail:<логин>` |
| Видео/файл урока не открывается | контейнер `minio` (это RustFS, имя историческое) не запущен: `docker compose --profile app ps` |
| Всё сломалось и непонятно что | `docker compose --profile app down -v`, затем шаг 4 заново |

Перед тем как звать преподавателя, приложите вывод `docker compose --profile app ps` и логи упавшего сервиса.
