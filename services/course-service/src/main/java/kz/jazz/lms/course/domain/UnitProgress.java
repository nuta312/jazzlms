package kz.jazz.lms.course.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "unit_progress")
public class UnitProgress {
    @Id
    private UUID id;
    @Column(name = "unit_id", nullable = false)
    private UUID unitId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();
    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void prePersist() { if (id == null) id = UUID.randomUUID(); }

    public UUID getId() { return id; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
