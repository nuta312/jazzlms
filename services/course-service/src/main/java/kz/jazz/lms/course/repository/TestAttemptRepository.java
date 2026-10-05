package kz.jazz.lms.course.repository;

import kz.jazz.lms.course.domain.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, UUID> {
    List<TestAttempt> findByTestIdAndUserIdOrderByStartedAtDesc(UUID testId, UUID userId);
    List<TestAttempt> findByTestIdOrderByStartedAtDesc(UUID testId);

    /** Score в отчёте по курсу: средний по тестам курса лучший результат каждого ученика. [user_id, avg]. */
    @Query(value = """
            select t.user_id, avg(t.best) from (
              select a.user_id, a.test_id, max(a.score) as best
              from test_attempts a join units u on u.id = a.test_id
              where u.course_id = :courseId and a.score is not null
              group by a.user_id, a.test_id) t
            group by t.user_id""", nativeQuery = true)
    List<Object[]> bestScorePerUser(UUID courseId);
}
