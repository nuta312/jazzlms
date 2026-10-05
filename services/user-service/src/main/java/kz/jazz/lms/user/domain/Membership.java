package kz.jazz.lms.user.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Составной ключ таблиц-связок (user_branches, user_groups): пара userId + targetId.
 * JPA-правило: класс ключа — обычный Serializable с equals/hashCode, а поля с теми же именами
 * помечены @Id прямо в сущности (наследовать их из @MappedSuperclass Hibernate не разрешает).
 */
public final class Membership {
    private Membership() {}

    public static class Key implements Serializable {
        private UUID userId;
        private UUID targetId;
        public Key() {}
        public Key(UUID userId, UUID targetId) { this.userId = userId; this.targetId = targetId; }
        @Override public boolean equals(Object o) { return o instanceof Key k && Objects.equals(k.userId, userId) && Objects.equals(k.targetId, targetId); }
        @Override public int hashCode() { return Objects.hash(userId, targetId); }
    }
}
