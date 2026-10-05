package kz.jazz.lms.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kz.jazz.lms.course.domain.Test;

import java.util.List;
import java.util.UUID;

/** Тело POST /api/courses/{id}/tests и PUT /api/units/{id}/test: название урока, настройки, состав. */
public record TestRequest(
        @NotBlank @Size(max = 80) String name,
        Boolean active,
        @Min(1) @Max(600) Integer durationMinutes,
        @Min(0) @Max(100) Integer passScore,
        Boolean shuffleQuestions, Boolean shuffleAnswers,
        Test.Repetitions repetitions, @Min(1) Integer maxAttempts,
        Test.ShowCorrect showCorrectAnswers, Boolean showGivenAnswers, Boolean showLabels, Boolean showScore,
        @Size(max = 800) String description, @Size(max = 2000) String messagePassed, @Size(max = 2000) String messageFailed,
        List<Item> questions
) {
    public record Item(UUID questionId, Integer weight) {}
}
