-- Тесты: банк вопросов курса, настройки теста, состав теста и попытки учеников.
-- Ответы/пары/варианты храним в JSONB: у каждого типа вопроса своя структура, а строк-таблиц на каждый тип не хочется.
CREATE TABLE questions (
    id          UUID PRIMARY KEY,
    course_id   UUID        NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    type        VARCHAR(20) NOT NULL,   -- MULTIPLE_CHOICE | FILL_GAP | ORDERING | DRAG_DROP | FREE_TEXT | RANDOMIZED
    text        TEXT        NOT NULL,   -- текст вопроса (для RANDOMIZED — название пула)
    data        JSONB       NOT NULL DEFAULT '{}',   -- answers / items / pairs / options / pool
    feedback    TEXT,
    tags        VARCHAR(255),
    created_by  UUID,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_questions_course ON questions (course_id);

-- Настройки теста: одна строка на урок типа TEST (Test options в TalentLMS).
CREATE TABLE tests (
    unit_id              UUID PRIMARY KEY REFERENCES units (id) ON DELETE CASCADE,
    duration_minutes     INTEGER,
    pass_score           INTEGER     NOT NULL DEFAULT 50,
    shuffle_questions    BOOLEAN     NOT NULL DEFAULT FALSE,
    shuffle_answers      BOOLEAN     NOT NULL DEFAULT FALSE,
    repetitions          VARCHAR(20) NOT NULL DEFAULT 'IF_NOT_PASSED',   -- ALWAYS | IF_NOT_PASSED | NEVER
    max_attempts         INTEGER,
    show_correct_answers VARCHAR(20) NOT NULL DEFAULT 'WHEN_PASSED',     -- ALWAYS | WHEN_PASSED | NEVER
    show_given_answers   BOOLEAN     NOT NULL DEFAULT TRUE,
    show_labels          BOOLEAN     NOT NULL DEFAULT TRUE,
    show_score           BOOLEAN     NOT NULL DEFAULT TRUE,
    description          VARCHAR(800),
    message_passed       TEXT,
    message_failed       TEXT
);

-- Состав теста: какие вопросы, в каком порядке и с каким весом.
CREATE TABLE test_questions (
    test_id     UUID    NOT NULL REFERENCES tests (unit_id) ON DELETE CASCADE,
    question_id UUID    NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    position    INTEGER NOT NULL,
    weight      INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (test_id, question_id)
);

-- Попытка ученика. questions — вопросы в том виде, в каком их показали (порядок, перемешанные варианты,
-- выбранный вопрос из Randomized-пула); answers — ответы; results — что засчитано.
CREATE TABLE test_attempts (
    id           UUID PRIMARY KEY,
    test_id      UUID      NOT NULL REFERENCES tests (unit_id) ON DELETE CASCADE,
    user_id      UUID      NOT NULL,
    started_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    submitted_at TIMESTAMP,
    score        INTEGER,
    passed       BOOLEAN,
    questions    JSONB     NOT NULL DEFAULT '[]',
    answers      JSONB,
    results      JSONB
);
CREATE INDEX idx_test_attempts_user ON test_attempts (user_id, test_id);
