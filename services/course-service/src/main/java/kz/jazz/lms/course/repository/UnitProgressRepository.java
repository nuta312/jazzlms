package kz.jazz.lms.course.repository;

import kz.jazz.lms.course.domain.UnitProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitProgressRepository extends JpaRepository<UnitProgress, UUID> {
    Optional<UnitProgress> findByUnitIdAndUserId(UUID unitId, UUID userId);
    List<UnitProgress> findByUserIdAndUnitIdIn(UUID userId, Collection<UUID> unitIds);

    /**
     * Training time = сумма (completed_at - started_at) по пройденным урокам.
     * Нативный SQL: арифметика над timestamp зависит от СУБД (extract(epoch ...) — это PostgreSQL), в JPQL её нет.
     */
    @Query(value = "select coalesce(sum(extract(epoch from (completed_at - started_at))), 0) from unit_progress where completed_at is not null",
            nativeQuery = true)
    double totalTrainingSeconds();

    @Query(value = "select coalesce(sum(extract(epoch from (completed_at - started_at))), 0) from unit_progress where completed_at is not null and user_id = :userId",
            nativeQuery = true)
    double trainingSecondsOf(UUID userId);

    @Query(value = "select coalesce(sum(extract(epoch from (p.completed_at - p.started_at))), 0) from unit_progress p join units u on u.id = p.unit_id where p.completed_at is not null and u.course_id = :courseId",
            nativeQuery = true)
    double trainingSecondsOfCourse(UUID courseId);

    /** Время каждого ученика в курсе: [user_id, seconds]. */
    @Query(value = "select p.user_id, coalesce(sum(extract(epoch from (p.completed_at - p.started_at))), 0) from unit_progress p join units u on u.id = p.unit_id where p.completed_at is not null and u.course_id = :courseId group by p.user_id",
            nativeQuery = true)
    List<Object[]> trainingSecondsPerUser(UUID courseId);
}
