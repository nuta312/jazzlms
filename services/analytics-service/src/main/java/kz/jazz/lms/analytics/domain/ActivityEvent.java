package kz.jazz.lms.analytics.domain;

import kz.jazz.lms.events.EventType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/** Лента активности ("Timeline"). Каждое событие из Kafka сохраняем как есть. */
@Document("activity_events")
public class ActivityEvent {
    @Id
    private String id;
    @Indexed(unique = true)
    private String eventId;            // защита от дублей при повторной доставке
    private EventType type;
    private String source;
    @Indexed
    private Instant occurredAt;
    private String userId;
    private String description;
    private Map<String, String> payload;

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, String> getPayload() { return payload; }
    public void setPayload(Map<String, String> payload) { this.payload = payload; }
}
