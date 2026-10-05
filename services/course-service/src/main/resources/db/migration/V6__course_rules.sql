-- Rules & Path: sequential = уроки открываются по порядку (следующий доступен после завершения предыдущего).
ALTER TABLE courses ADD COLUMN sequential BOOLEAN NOT NULL DEFAULT FALSE;
