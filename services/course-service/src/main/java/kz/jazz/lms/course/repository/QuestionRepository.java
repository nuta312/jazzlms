package kz.jazz.lms.course.repository;

import kz.jazz.lms.course.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findByCourseIdOrderByCreatedAtDesc(UUID courseId);
    List<Question> findByCourseIdInOrderByCreatedAtDesc(Collection<UUID> courseIds);
    List<Question> findAllByOrderByCreatedAtDesc();
}
