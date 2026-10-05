package kz.jazz.lms.notification.repository;

import kz.jazz.lms.events.EventType;
import kz.jazz.lms.notification.domain.NotificationRule;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** Spring Data Mongo: тот же подход, что и JPA, но запросы идут в MongoDB. */
public interface NotificationRuleRepository extends MongoRepository<NotificationRule, String> {
    List<NotificationRule> findByEventTypeAndActiveTrue(EventType eventType);
}
