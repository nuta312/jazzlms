package kz.jazz.lms.gamification.kafka;

import kz.jazz.lms.events.DomainEvent;
import kz.jazz.lms.events.Topics;
import kz.jazz.lms.gamification.service.GamificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventListener {
    private final GamificationService gamification;

    public EventListener(GamificationService gamification) { this.gamification = gamification; }

    @KafkaListener(topics = {Topics.USER_EVENTS, Topics.COURSE_EVENTS, Topics.ENROLLMENT_EVENTS})
    public void onEvent(DomainEvent event) { gamification.apply(event); }
}
