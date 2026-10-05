package kz.jazz.lms.course.repository;

import kz.jazz.lms.course.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findByDeletedAtIsNull();
    List<Course> findByCreatedByAndDeletedAtIsNull(UUID createdBy);
    List<Course> findByDeletedAtIsNotNull();
    List<Course> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name);
    List<Course> findByActiveTrueAndHiddenFromCatalogFalse();
    long countByCategoryId(UUID categoryId);
    long countByActiveTrueAndDeletedAtIsNull();
}
