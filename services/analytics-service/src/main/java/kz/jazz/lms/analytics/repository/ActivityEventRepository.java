package kz.jazz.lms.analytics.repository;

import kz.jazz.lms.analytics.domain.ActivityEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActivityEventRepository extends MongoRepository<ActivityEvent, String> {
    boolean existsByEventId(String eventId);
    List<ActivityEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
