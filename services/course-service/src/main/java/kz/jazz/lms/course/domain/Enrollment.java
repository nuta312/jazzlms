package kz.jazz.lms.course.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Запись пользователя на курс. user_id — id из user-service.
 * Внешнего ключа нет: таблица users живёт в другой базе, другого сервиса.
 * Проверяем существование пользователя через gRPC при создании.
 */
@Entity
@Table(name = "enrollments")
public class Enrollment {
    public enum Role { LEARNER, INSTRUCTOR }

    @Id
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.LEARNER;

    @Column(nullable = false)
    private int progress = 0;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void prePersist() { if (id == null) id = UUID.randomUUID(); }

    public UUID getId() { return id; }
    public UUID getCourseId() { return courseId; }
    public void setCourseId(UUID courseId) { this.courseId = courseId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public Instant getEnrolledAt() { return enrolledAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
