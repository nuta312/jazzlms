package kz.jazz.lms.course.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Выданный сертификат. Создаётся автоматически при завершении курса (progress = 100%). */
@Entity
@Table(name = "certificates")
public class Certificate {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "course_id", nullable = false) private UUID courseId;
    @Column(name = "template_id") private UUID templateId;
    @Column(nullable = false, unique = true) private String code;
    @Column(name = "issued_at", nullable = false) private Instant issuedAt = Instant.now();

    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getCourseId() { return courseId; }
    public void setCourseId(UUID courseId) { this.courseId = courseId; }
    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Instant getIssuedAt() { return issuedAt; }
}
