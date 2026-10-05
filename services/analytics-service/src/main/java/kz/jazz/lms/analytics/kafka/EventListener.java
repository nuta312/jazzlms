package kz.jazz.lms.analytics.kafka;

import kz.jazz.lms.analytics.service.StatsService;
import kz.jazz.lms.events.DomainEvent;
import kz.jazz.lms.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Тот же топик читают notification-service и analytics-service — у них разные group-id,
 * поэтому Kafka доставляет каждое сообщение обоим (publish/subscribe).
 */
@Component
public class EventListener {
    private static final Logger log = LoggerFactory.getLogger(EventListener.class);
    private final StatsService stats;

    public EventListener(StatsService stats) { this.stats = stats; }

    @KafkaListener(topics = {Topics.USER_EVENTS, Topics.COURSE_EVENTS, Topics.ENROLLMENT_EVENTS})
    public void onEvent(DomainEvent event) {
        log.info("Recording {} ({})", event.type(), event.eventId());
        stats.record(event);
    }
}
