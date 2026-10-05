package kz.jazz.lms.gamification.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Идемпотентность: Kafka гарантирует доставку "хотя бы раз", а очки нельзя начислять дважды.
 * Запоминаем eventId; TTL-индекс удаляет записи через 7 дней.
 */
@Document("processed_events")
public class ProcessedEvent {
    @Id
    private String eventId;
    @Indexed(expireAfter = "7d")
    private Instant processedAt = Instant.now();

    public ProcessedEvent() {}
    public ProcessedEvent(String eventId) { this.eventId = eventId; }
    public String getEventId() { return eventId; }
    public Instant getProcessedAt() { return processedAt; }
}
