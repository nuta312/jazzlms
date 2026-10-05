-- Уроки (units) курса: видео, презентации/документы, текстовый контент.
-- Сам файл лежит в объектном хранилище MinIO (S3), в БД — только ключ и метаданные.
CREATE TABLE units (
    id                 UUID PRIMARY KEY,
    course_id          UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    type               VARCHAR(20)  NOT NULL,            -- VIDEO | PRESENTATION | CONTENT
    name               VARCHAR(80)  NOT NULL,
    position           INTEGER      NOT NULL DEFAULT 0,  -- порядок в курсе
    active             BOOLEAN      NOT NULL DEFAULT TRUE,

    completion_type    VARCHAR(20)  NOT NULL DEFAULT 'CHECKBOX',  -- CHECKBOX | QUESTION | TIME
    time_limit_seconds INTEGER,
    question           VARCHAR(500),
    answer             VARCHAR(200),

    source_type        VARCHAR(20),                      -- YOUTUBE | UPLOAD (для CONTENT — NULL)
    youtube_url        VARCHAR(500),
    text_content       TEXT,
    file_key           VARCHAR(500),                     -- ключ объекта в бакете MinIO
    file_name          VARCHAR(255),
    file_content_type  VARCHAR(150),
    file_size          BIGINT,

    created_by         UUID,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_units_course ON units (course_id, position);

-- Прохождение урока конкретным пользователем.
CREATE TABLE unit_progress (
    id           UUID PRIMARY KEY,
    unit_id      UUID      NOT NULL REFERENCES units (id) ON DELETE CASCADE,
    user_id      UUID      NOT NULL,
    started_at   TIMESTAMP NOT NULL DEFAULT NOW(),   -- нужен для completion_type = TIME
    completed_at TIMESTAMP,
    UNIQUE (unit_id, user_id)
);
CREATE INDEX idx_unit_progress_user ON unit_progress (user_id);
