package kz.jazz.lms.user.dto;

import jakarta.validation.constraints.*;
import kz.jazz.lms.user.domain.UserType;

/** Тело запроса POST /api/users. Аннотации валидации проверяются автоматически (@Valid). */
public record CreateUserRequest(
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 3, max = 50) String username,
        @Size(min = 6, max = 72) String password,     // null -> сгенерируем случайный
        UserType userType,
        String timeZone,
        String language,
        @Size(max = 800) String bio,
        Boolean active,
        Boolean excludeFromEmails
) {}
