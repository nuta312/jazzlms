package kz.jazz.lms.notification.domain;

import kz.jazz.lms.events.EventType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Правило уведомления: "когда происходит событие X — отправить письмо Y".
 * Это MongoDB-документ: коллекция notification_rules, JSON-подобная структура.
 */
@Document("notification_rules")
public class NotificationRule {
    @Id
    private String id;
    private String name;
    private EventType eventType;
    private Recipient recipient;
    private boolean active = true;
    private int delayMinutes = 0;        // 0 = сразу; >0 = отложенное ("X hours after ...")
    private String subjectTemplate;      // поддерживает {{firstName}}, {{courseName}} и т.п.
    private String bodyTemplate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public Recipient getRecipient() { return recipient; }
    public void setRecipient(Recipient recipient) { this.recipient = recipient; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }
    public String getSubjectTemplate() { return subjectTemplate; }
    public void setSubjectTemplate(String subjectTemplate) { this.subjectTemplate = subjectTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
}
