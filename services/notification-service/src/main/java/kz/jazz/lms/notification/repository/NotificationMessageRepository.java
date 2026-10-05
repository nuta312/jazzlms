package kz.jazz.lms.notification.repository;

import kz.jazz.lms.notification.domain.NotificationMessage;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface NotificationMessageRepository extends MongoRepository<NotificationMessage, String> {
    List<NotificationMessage> findByStatus(NotificationMessage.Status status, Sort sort);
    List<NotificationMessage> findByStatusAndScheduledAtBefore(NotificationMessage.Status status, Instant before);
    void deleteByStatus(NotificationMessage.Status status);
}
