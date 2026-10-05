-- Ветки (Branches) — под-порталы со своими настройками, и группы (Groups) — наборы пользователей.
-- Пользователь может состоять в нескольких ветках и группах: связь many-to-many через таблицы-связки.
CREATE TABLE branches (
    id                UUID PRIMARY KEY,
    name              VARCHAR(60)  NOT NULL UNIQUE,   -- короткое имя, как поддомен: marketing
    title             VARCHAR(120),
    description       VARCHAR(255),
    language          VARCHAR(10)  NOT NULL DEFAULT 'en',
    time_zone         VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    default_user_type VARCHAR(20)  NOT NULL DEFAULT 'LEARNER',
    signup_mode       VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',  -- MANUAL | DIRECT
    announcement      VARCHAR(2000),
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE user_branches (
    user_id   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches (id) ON DELETE CASCADE,
    added_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, branch_id)
);

CREATE TABLE groups (
    id          UUID PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL UNIQUE,
    description VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE user_groups (
    user_id  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, group_id)
);
