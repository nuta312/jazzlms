package kz.jazz.lms.user.repository;

import kz.jazz.lms.user.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
    boolean existsByNameIgnoreCase(String name);
}
