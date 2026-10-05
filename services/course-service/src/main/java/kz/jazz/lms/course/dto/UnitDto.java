package kz.jazz.lms.course.dto;

import kz.jazz.lms.course.domain.Unit;

import java.time.Instant;
import java.util.UUID;

/**
 * answer отдаём только преподавателю (ученику — null, иначе ответ виден в DevTools).
 * fileUrl — временная (presigned) ссылка на файл в MinIO, заполняется только при открытии урока.
 */
public record UnitDto(
        UUID id, UUID courseId, Unit.Type type, String name, int position, boolean active,
        Unit.CompletionType completionType, Integer timeLimitSeconds, String question, String answer,
        Unit.SourceType sourceType, String youtubeUrl, String embedUrl, String textContent,
        String fileName, String fileContentType, Long fileSize, String fileUrl,
        Instant createdAt, boolean completed, Instant startedAt,
        boolean autoplay, boolean showSpeed, String description,
        boolean locked           // Rules & Path: предыдущий урок ещё не пройден
) {}
