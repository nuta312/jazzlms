package kz.jazz.lms.user.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Строка таблицы-связки user_branches: пользователь состоит в ветке. */
@Entity
@Table(name = "user_branches")
@IdClass(Membership.Key.class)
public class UserBranch {
    @Id @Column(name = "user_id") private UUID userId;
    @Id @Column(name = "branch_id") private UUID targetId;
    @Column(name = "added_at", nullable = false) private Instant addedAt = Instant.now();

    public UserBranch() {}
    public UserBranch(UUID userId, UUID branchId) { this.userId = userId; this.targetId = branchId; }
    public UUID getUserId() { return userId; }
    public UUID getBranchId() { return targetId; }
    public Instant getAddedAt() { return addedAt; }
}
