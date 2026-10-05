-- Flyway: миграции применяются по порядку версий (V1, V2, ...)
-- и записываются в таблицу flyway_schema_history.
CREATE TABLE users (
    id            UUID PRIMARY KEY,
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    user_type     VARCHAR(20)  NOT NULL,
    time_zone     VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    language      VARCHAR(10)  NOT NULL DEFAULT 'en',
    bio           VARCHAR(800),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    exclude_from_emails BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMP
);

CREATE INDEX idx_users_user_type ON users (user_type);
