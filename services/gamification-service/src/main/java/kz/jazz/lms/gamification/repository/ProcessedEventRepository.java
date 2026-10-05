package kz.jazz.lms.gamification.repository;

import kz.jazz.lms.gamification.domain.ProcessedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessedEventRepository extends MongoRepository<ProcessedEvent, String> {}
