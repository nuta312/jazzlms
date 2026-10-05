package kz.jazz.lms.notification.kafka;

import kz.jazz.lms.events.DomainEvent;
import kz.jazz.lms.events.Topics;
import kz.jazz.lms.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer. Подписан сразу на три топика.
 * Spring сам десериализует JSON в DomainEvent (см. application.yml).
 */
@Component
public class EventListener {
    private static final Logger log = LoggerFactory.getLogger(EventListener.class);

    private final NotificationService notifications;

    public EventListener(NotificationService notifications) { this.notifications = notifications; }

    @KafkaListener(topics = {Topics.USER_EVENTS, Topics.COURSE_EVENTS, Topics.ENROLLMENT_EVENTS})
    public void onEvent(DomainEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received {} from {}[{}]@{} payload={}", event.type(), topic, partition, offset, event.payload());
        notifications.handle(event);
    }
}
