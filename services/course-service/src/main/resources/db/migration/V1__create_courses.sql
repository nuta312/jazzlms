CREATE TABLE categories (
    id        UUID PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    parent_id UUID REFERENCES categories (id) ON DELETE SET NULL,
    price     NUMERIC(10, 2)
);

CREATE TABLE courses (
    id           UUID PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    code         VARCHAR(20),
    description  VARCHAR(5000),
    category_id  UUID REFERENCES categories (id) ON DELETE SET NULL,
    price        NUMERIC(10, 2),
    capacity     INTEGER,
    level        VARCHAR(20),
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    hidden_from_catalog BOOLEAN NOT NULL DEFAULT FALSE,
    created_by   UUID,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE enrollments (
    id           UUID PRIMARY KEY,
    course_id    UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    user_id      UUID NOT NULL,                -- ссылка на users в другом сервисе (без FK!)
    role         VARCHAR(20) NOT NULL,         -- LEARNER | INSTRUCTOR
    progress     INTEGER NOT NULL DEFAULT 0,   -- 0..100
    enrolled_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    UNIQUE (course_id, user_id)
);

CREATE INDEX idx_enrollments_user ON enrollments (user_id);
