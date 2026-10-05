package kz.jazz.lms.course.service;

import kz.jazz.lms.events.DomainEvent;
import kz.jazz.lms.events.EventType;
import kz.jazz.lms.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CourseEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(CourseEventPublisher.class);
    private final KafkaTemplate<String, Object> kafka;

    public CourseEventPublisher(KafkaTemplate<String, Object> kafka) { this.kafka = kafka; }

    public void publish(String topic, EventType type, String key, Map<String, String> payload) {
        DomainEvent event = DomainEvent.of(type, "course-service", payload);
        kafka.send(topic, key, event).whenComplete((r, ex) -> {
            if (ex != null) log.error("Failed to publish {}: {}", type, ex.getMessage());
            else log.info("Published {} to {}", type, topic);
        });
    }
}
