package kz.jazz.lms.course.dto;

import kz.jazz.lms.course.domain.Enrollment;

import java.time.Instant;
import java.util.UUID;

/** Данные пользователя (имя, email) подтянуты по gRPC из user-service. */
public record EnrollmentDto(
        UUID id, UUID courseId, UUID userId,
        String userFullName, String userEmail,
        Enrollment.Role role, int progress, Instant enrolledAt, Instant completedAt
) {
    public static EnrollmentDto from(Enrollment e, String fullName, String email) {
        return new EnrollmentDto(e.getId(), e.getCourseId(), e.getUserId(), fullName, email,
                e.getRole(), e.getProgress(), e.getEnrolledAt(), e.getCompletedAt());
    }
}
