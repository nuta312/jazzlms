package kz.jazz.lms.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Универсальный "конверт" события.
 *
 * @param eventId    уникальный id (для идемпотентности consumer'а)
 * @param type       тип события
 * @param occurredAt когда произошло
 * @param source     какой сервис отправил
 * @param payload    данные события (userId, email, courseId, ...)
 */
public record DomainEvent(
        UUID eventId,
        EventType type,
        Instant occurredAt,
        String source,
        Map<String, String> payload
) {
    public static DomainEvent of(EventType type, String source, Map<String, String> payload) {
        return new DomainEvent(UUID.randomUUID(), type, Instant.now(), source, payload);
    }

    public String get(String key) {
        return payload == null ? null : payload.get(key);
    }
}
