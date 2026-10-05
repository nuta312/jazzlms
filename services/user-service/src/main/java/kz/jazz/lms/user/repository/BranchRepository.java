package kz.jazz.lms.user.repository;

import kz.jazz.lms.user.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
    boolean existsByNameIgnoreCase(String name);
}
