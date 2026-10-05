package kz.jazz.lms.course.repository;

import kz.jazz.lms.course.domain.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UnitRepository extends JpaRepository<Unit, UUID> {
    List<Unit> findByCourseIdOrderByPositionAsc(UUID courseId);
    List<Unit> findByCourseIdAndActiveTrue(UUID courseId);
    long countByCourseId(UUID courseId);
    List<Unit> findByCreatedByAndFileKeyIsNotNullOrderByCreatedAtDesc(UUID createdBy);
}
