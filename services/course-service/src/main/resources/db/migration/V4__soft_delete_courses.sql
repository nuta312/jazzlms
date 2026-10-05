-- Мягкое удаление: курс не стирается, а помечается. Это даёт "Undo delete" в отчёте Timeline.
-- Настоящее удаление (вместе с уроками и файлами) — отдельное действие "Permanently delete".
ALTER TABLE courses ADD COLUMN deleted_at TIMESTAMP;
CREATE INDEX idx_courses_deleted_at ON courses (deleted_at);
