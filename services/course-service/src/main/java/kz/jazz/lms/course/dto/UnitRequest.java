package kz.jazz.lms.course.dto;

import jakarta.validation.constraints.*;
import kz.jazz.lms.course.domain.Unit;
import java.util.UUID;

/**
 * JSON-часть multipart-запроса на создание урока (вторая часть — сам файл).
 * Форма "Add Video" в TalentLMS: Unit name, How to complete it, Select a video.
 */
public record UnitRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull Unit.Type type,
        Unit.CompletionType completionType,
        @Min(1) @Max(86400) Integer timeLimitSeconds,
        @Size(max = 500) String question,
        @Size(max = 200) String answer,
        Unit.SourceType sourceType,
        @Size(max = 500) String youtubeUrl,
        @Size(max = 20000) String textContent,
        Boolean active,
        Boolean autoplay,
        Boolean showSpeed,
        @Size(max = 20000) String description,
        UUID sourceUnitId        // "Use a document from your files": взять файл уже загруженного урока
) {}
