package kz.jazz.lms.user.repository;

import kz.jazz.lms.user.domain.Membership;
import kz.jazz.lms.user.domain.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserGroupRepository extends JpaRepository<UserGroup, Membership.Key> {
    List<UserGroup> findByUserId(UUID userId);
    List<UserGroup> findByTargetId(UUID groupId);
    long countByTargetId(UUID groupId);
}
