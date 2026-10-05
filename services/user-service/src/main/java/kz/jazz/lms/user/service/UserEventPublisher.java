package kz.jazz.lms.user.service;

import kz.jazz.lms.events.DomainEvent;
import kz.jazz.lms.events.EventType;
import kz.jazz.lms.events.Topics;
import kz.jazz.lms.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Отправка событий в Kafka. user-service не знает, кто их читает —
 * notification-service, analytics-service или кто-то ещё. Это и есть слабая связанность.
 */
@Component
public class UserEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Аудит: кто под кем вошёл. userId события — администратор (он действующее лицо). */
    public void publishImpersonation(User admin, User target) {
        DomainEvent event = DomainEvent.of(EventType.USER_IMPERSONATED, "user-service", Map.of(
                "userId", admin.getId().toString(),
                "firstName", admin.getFirstName(), "lastName", admin.getLastName(), "email", admin.getEmail(),
                "targetUserId", target.getId().toString(),
                "targetName", target.getFirstName() + " " + target.getLastName()));
        kafkaTemplate.send(Topics.USER_EVENTS, admin.getId().toString(), event);
    }

    public void publish(EventType type, User user) {
        DomainEvent event = DomainEvent.of(type, "user-service", Map.of(
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "username", user.getUsername(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "userType", user.getUserType().name()
        ));
        // key = userId: все события одного пользователя попадут в одну партицию (сохраняется порядок)
        kafkaTemplate.send(Topics.USER_EVENTS, user.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) log.error("Failed to publish {}: {}", type, ex.getMessage());
                    else log.info("Published {} to {} partition={}", type, Topics.USER_EVENTS,
                            result.getRecordMetadata().partition());
                });
    }
}
