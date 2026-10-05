-- Настройки плеера и описание урока (как Options в форме Edit unit в TalentLMS).
ALTER TABLE units ADD COLUMN autoplay   BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE units ADD COLUMN show_speed BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE units ADD COLUMN description TEXT;
