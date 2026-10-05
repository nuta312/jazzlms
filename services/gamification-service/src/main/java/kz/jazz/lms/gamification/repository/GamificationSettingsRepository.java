package kz.jazz.lms.gamification.repository;

import kz.jazz.lms.gamification.domain.GamificationSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GamificationSettingsRepository extends MongoRepository<GamificationSettings, String> {}
