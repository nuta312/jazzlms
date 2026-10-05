package kz.jazz.lms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupRequest(@NotBlank @Size(max = 80) String name, @Size(max = 500) String description, Boolean active) {}
