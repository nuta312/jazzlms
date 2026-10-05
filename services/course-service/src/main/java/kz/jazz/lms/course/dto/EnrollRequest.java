package kz.jazz.lms.course.dto;

import jakarta.validation.constraints.NotNull;
import kz.jazz.lms.course.domain.Enrollment;

import java.util.UUID;

public record EnrollRequest(@NotNull UUID userId, Enrollment.Role role) {}
