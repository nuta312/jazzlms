package kz.jazz.lms.course.repository;

import kz.jazz.lms.course.domain.CertificateTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, UUID> {
    List<CertificateTemplate> findAllByOrderByCreatedAtAsc();
    Optional<CertificateTemplate> findFirstByIsDefaultTrue();
}
