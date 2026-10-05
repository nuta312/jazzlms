package kz.jazz.lms.user.dto;

import jakarta.validation.constraints.*;

/** Саморегистрация ("Signup: Direct" в настройках). Роль берётся из Default user type. */
public record RegisterRequest(
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 6, max = 72) String password,
        Boolean acceptTerms      // обязателен, если в настройках задан текст Terms of Service
) {}
