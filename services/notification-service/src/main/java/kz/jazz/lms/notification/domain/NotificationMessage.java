package kz.jazz.lms.notification.domain;

import kz.jazz.lms.events.EventType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** История/очередь уведомлений (вкладки History и Pending notifications). */
@Document("notification_messages")
public class NotificationMessage {
    public enum Status { PENDING, SENT, FAILED }

    @Id
    private String id;
    private String ruleId;
    private String ruleName;
    private EventType eventType;
    private String recipientEmail;
    private String subject;
    private String body;
    @Indexed
    private Status status;
    private Instant createdAt = Instant.now();
    private Instant scheduledAt;
    private Instant sentAt;

    public String getId() { return id; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
