package kz.jazz.lms.course.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Шаблон сертификата: фон (пресет), заголовок и текст с плейсхолдерами. */
@Entity
@Table(name = "certificate_templates")
public class CertificateTemplate {
    public static final UUID CLASSIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000c01");
    public static final String DEFAULT_TITLE = "Certificate of Completion";
    public static final String DEFAULT_BODY = "This is to certify that {user} has successfully completed the course {course} on {date}.";

    @Id private UUID id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String background = "classic";
    @Column(nullable = false) private String title = DEFAULT_TITLE;
    @Column(nullable = false) private String body = DEFAULT_BODY;
    private String signature;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBackground() { return background; }
    public void setBackground(String background) { this.background = background; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean d) { this.isDefault = d; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
