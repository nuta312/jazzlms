package kz.jazz.lms.course.dto;

import kz.jazz.lms.course.domain.Enrollment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Курс текущего пользователя вместе с его ролью в курсе и прогрессом. */
/** instructors — имена преподавателей курса (бейдж INSTRUCTOR на главной ученика). */
public record MyEnrollmentDto(UUID enrollmentId, Enrollment.Role role, int progress, Instant enrolledAt, Instant completedAt,
                              CourseDto course, List<String> instructors) {}
