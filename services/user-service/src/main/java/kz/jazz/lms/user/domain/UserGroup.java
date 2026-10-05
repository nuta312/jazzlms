package kz.jazz.lms.user.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Строка таблицы-связки user_groups: пользователь состоит в группе. */
@Entity
@Table(name = "user_groups")
@IdClass(Membership.Key.class)
public class UserGroup {
    @Id @Column(name = "user_id") private UUID userId;
    @Id @Column(name = "group_id") private UUID targetId;
    @Column(name = "added_at", nullable = false) private Instant addedAt = Instant.now();

    public UserGroup() {}
    public UserGroup(UUID userId, UUID groupId) { this.userId = userId; this.targetId = groupId; }
    public UUID getUserId() { return userId; }
    public UUID getGroupId() { return targetId; }
    public Instant getAddedAt() { return addedAt; }
}
