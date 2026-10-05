package kz.jazz.lms.gamification.repository;

import kz.jazz.lms.gamification.domain.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProfileRepository extends MongoRepository<Profile, String> {}
