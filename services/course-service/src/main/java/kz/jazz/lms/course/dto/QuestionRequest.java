package kz.jazz.lms.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.jazz.lms.course.domain.Question;

import java.util.Map;

public record QuestionRequest(@NotNull Question.Type type,
                              @NotBlank @Size(max = 5000) String text,
                              Map<String, Object> data,
                              @Size(max = 2000) String feedback,
                              @Size(max = 255) String tags) {}
