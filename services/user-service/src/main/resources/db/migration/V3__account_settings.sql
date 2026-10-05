-- Account & Settings: одна строка настроек портала (id = 1).
-- Каждый сервис хранит СВОИ настройки: user-service — регистрацию и пароли,
-- gamification-service — очки/бейджи (MongoDB), course-service — шаблоны сертификатов.
CREATE TABLE account_settings (
    id                          SMALLINT PRIMARY KEY,
    site_name                   VARCHAR(100) NOT NULL DEFAULT 'JazzLMS',
    site_description            VARCHAR(500),
    default_language            VARCHAR(10)  NOT NULL DEFAULT 'en',
    default_time_zone           VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    signup_mode                 VARCHAR(20)  NOT NULL DEFAULT 'DIRECT',      -- MANUAL | DIRECT
    default_user_type           VARCHAR(20)  NOT NULL DEFAULT 'LEARNER',
    default_group_id            UUID,                                         -- новые пользователи попадают в группу
    password_max_age_days       INTEGER,                                      -- NULL = не требовать смену
    password_change_first_login BOOLEAN      NOT NULL DEFAULT FALSE,
    lock_after_attempts         INTEGER,                                      -- NULL = не блокировать
    lock_minutes                INTEGER      NOT NULL DEFAULT 30,
    terms_of_service            TEXT,
    visible_user_format         VARCHAR(20)  NOT NULL DEFAULT 'FIRST_LAST',   -- FIRST_LAST | LAST_FIRST | USERNAME
    updated_at                  TIMESTAMP    NOT NULL DEFAULT NOW()
);
INSERT INTO account_settings (id) VALUES (1);

-- Политика паролей: когда пароль менялся и нужно ли сменить при следующем входе.
ALTER TABLE users ADD COLUMN password_changed_at  TIMESTAMP;
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
