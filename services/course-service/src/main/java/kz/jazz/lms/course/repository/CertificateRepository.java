package kz.jazz.lms.course.repository;

import kz.jazz.lms.course.domain.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    List<Certificate> findByUserIdOrderByIssuedAtDesc(UUID userId);
    Optional<Certificate> findByUserIdAndCourseId(UUID userId, UUID courseId);
    List<Certificate> findByCourseId(UUID courseId);
}
