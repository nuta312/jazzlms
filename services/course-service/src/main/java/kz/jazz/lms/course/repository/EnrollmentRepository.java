package kz.jazz.lms.course.repository;

import kz.jazz.lms.course.domain.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    List<Enrollment> findByCourseId(UUID courseId);
    List<Enrollment> findByUserId(UUID userId);
    List<Enrollment> findByCourseIdInAndRole(Collection<UUID> courseIds, Enrollment.Role role);
    Optional<Enrollment> findByCourseIdAndUserId(UUID courseId, UUID userId);
    long countByCourseIdAndRole(UUID courseId, Enrollment.Role role);

    // --- показатели для главной. Подзапрос отсекает записи на мягко удалённые курсы. ---
    String LIVE = " and e.courseId in (select c.id from Course c where c.deletedAt is null)";

    @Query("select count(e) from Enrollment e where e.role = 'LEARNER'" + LIVE)
    long countAssigned();

    @Query("select count(e) from Enrollment e where e.role = 'LEARNER' and e.completedAt is not null" + LIVE)
    long countCompleted();

    @Query("select count(e) from Enrollment e where e.role = 'LEARNER' and e.completedAt is null and e.progress > 0" + LIVE)
    long countInProgress();
}
