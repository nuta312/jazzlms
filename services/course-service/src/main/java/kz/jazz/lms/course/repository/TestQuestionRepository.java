package kz.jazz.lms.course.repository;

import kz.jazz.lms.course.domain.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestQuestionRepository extends JpaRepository<TestQuestion, TestQuestion.Key> {
    List<TestQuestion> findByTestIdOrderByPositionAsc(UUID testId);
    void deleteByTestId(UUID testId);
    long countByQuestionId(UUID questionId);
}
