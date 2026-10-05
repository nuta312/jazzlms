-- Сертификаты: шаблоны (Account & Settings → Certificates) и выданные сертификаты.
CREATE TABLE certificate_templates (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    background  VARCHAR(30)  NOT NULL DEFAULT 'classic',   -- ключ пресета фона (рисуется CSS, файлов нет)
    title       VARCHAR(200) NOT NULL,
    body        TEXT         NOT NULL,                     -- текст с плейсхолдерами {user} {course} {date} {code}
    signature   VARCHAR(100),
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

INSERT INTO certificate_templates (id, name, background, title, body, signature, is_default) VALUES
 ('00000000-0000-0000-0000-000000000c01', 'Classic', 'classic', 'Certificate of Completion',
  'This is to certify that {user} has successfully completed the course {course} on {date}.', 'JazzLMS Academy', TRUE);

-- Один сертификат на пару (пользователь, курс). user_id — из user-service, без FK (другая база).
CREATE TABLE certificates (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    course_id   UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    template_id UUID REFERENCES certificate_templates (id) ON DELETE SET NULL,
    code        VARCHAR(24) NOT NULL UNIQUE,               -- номер для проверки подлинности
    issued_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, course_id)
);
CREATE INDEX idx_certificates_user ON certificates (user_id);

-- Какой шаблон выдаёт курс (NULL = без сертификата). Существующим курсам назначаем Classic.
ALTER TABLE courses ADD COLUMN certificate_template_id UUID REFERENCES certificate_templates (id) ON DELETE SET NULL;
UPDATE courses SET certificate_template_id = '00000000-0000-0000-0000-000000000c01';
