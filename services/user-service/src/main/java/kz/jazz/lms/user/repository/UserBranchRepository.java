package kz.jazz.lms.user.repository;

import kz.jazz.lms.user.domain.Membership;
import kz.jazz.lms.user.domain.UserBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserBranchRepository extends JpaRepository<UserBranch, Membership.Key> {
    List<UserBranch> findByUserId(UUID userId);
    List<UserBranch> findByTargetId(UUID branchId);
    long countByTargetId(UUID branchId);
}
