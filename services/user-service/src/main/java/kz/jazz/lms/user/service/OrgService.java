package kz.jazz.lms.user.service;

import kz.jazz.lms.user.domain.*;
import kz.jazz.lms.user.dto.BranchRequest;
import kz.jazz.lms.user.dto.GroupRequest;
import kz.jazz.lms.user.dto.UserDto;
import kz.jazz.lms.user.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Ветки и группы + членство пользователей. Один сервис, потому что логика симметричная. */
@Service
@Transactional
public class OrgService {
    private final BranchRepository branches;
    private final GroupRepository groups;
    private final UserBranchRepository userBranches;
    private final UserGroupRepository userGroups;
    private final UserRepository users;

    public OrgService(BranchRepository branches, GroupRepository groups, UserBranchRepository userBranches,
                      UserGroupRepository userGroups, UserRepository users) {
        this.branches = branches; this.groups = groups; this.userBranches = userBranches; this.userGroups = userGroups; this.users = users;
    }

    // ---------- branches ----------

    public record BranchView(Branch branch, long members) {}

    @Transactional(readOnly = true)
    public List<BranchView> allBranches() {
        return branches.findAll().stream().sorted(Comparator.comparing(Branch::getName))
                .map(b -> new BranchView(b, userBranches.countByTargetId(b.getId()))).toList();
    }

    public Branch getBranch(UUID id) { return branches.findById(id).orElseThrow(() -> new NotFoundException("Branch not found: " + id)); }

    public Branch createBranch(BranchRequest req) {
        if (branches.existsByNameIgnoreCase(req.name())) throw new ConflictException("Branch name already taken: " + req.name());
        return branches.save(apply(new Branch(), req));
    }

    public Branch updateBranch(UUID id, BranchRequest req) {
        Branch b = getBranch(id);
        if (!b.getName().equalsIgnoreCase(req.name()) && branches.existsByNameIgnoreCase(req.name()))
            throw new ConflictException("Branch name already taken: " + req.name());
        return branches.save(apply(b, req));
    }

    public void deleteBranch(UUID id) { branches.delete(getBranch(id)); }

    private Branch apply(Branch b, BranchRequest r) {
        b.setName(r.name().trim());
        b.setTitle(r.title());
        b.setDescription(r.description());
        if (r.language() != null) b.setLanguage(r.language());
        if (r.timeZone() != null) b.setTimeZone(r.timeZone());
        if (r.defaultUserType() != null) b.setDefaultUserType(r.defaultUserType());
        if (r.signupMode() != null) b.setSignupMode(r.signupMode());
        b.setAnnouncement(r.announcement());
        if (r.active() != null) b.setActive(r.active());
        return b;
    }

    @Transactional(readOnly = true)
    public List<Branch> branchesOfUser(UUID userId) {
        return userBranches.findByUserId(userId).stream().map(m -> branches.findById(m.getBranchId()).orElse(null)).filter(Objects::nonNull).toList();
    }

    @Transactional(readOnly = true)
    public List<UserDto> membersOfBranch(UUID branchId) {
        getBranch(branchId);
        return userBranches.findByTargetId(branchId).stream().map(m -> users.findById(m.getUserId()).map(UserDto::from).orElse(null)).filter(Objects::nonNull).toList();
    }

    public void addUserToBranch(UUID branchId, UUID userId) {
        getBranch(branchId);
        if (!users.existsById(userId)) throw new NotFoundException("User not found: " + userId);
        userBranches.save(new UserBranch(userId, branchId));   // повторное добавление — просто перезапишет ту же строку
    }

    public void removeUserFromBranch(UUID branchId, UUID userId) { userBranches.deleteById(new Membership.Key(userId, branchId)); }

    // ---------- groups ----------

    public record GroupView(Group group, long members) {}

    @Transactional(readOnly = true)
    public List<GroupView> allGroups() {
        return groups.findAll().stream().sorted(Comparator.comparing(Group::getName))
                .map(g -> new GroupView(g, userGroups.countByTargetId(g.getId()))).toList();
    }

    public Group getGroup(UUID id) { return groups.findById(id).orElseThrow(() -> new NotFoundException("Group not found: " + id)); }

    public Group createGroup(GroupRequest req) {
        if (groups.existsByNameIgnoreCase(req.name())) throw new ConflictException("Group name already taken: " + req.name());
        Group g = new Group();
        return groups.save(applyGroup(g, req));
    }

    public Group updateGroup(UUID id, GroupRequest req) {
        Group g = getGroup(id);
        if (!g.getName().equalsIgnoreCase(req.name()) && groups.existsByNameIgnoreCase(req.name()))
            throw new ConflictException("Group name already taken: " + req.name());
        return groups.save(applyGroup(g, req));
    }

    public void deleteGroup(UUID id) { groups.delete(getGroup(id)); }

    private Group applyGroup(Group g, GroupRequest r) {
        g.setName(r.name().trim());
        g.setDescription(r.description());
        if (r.active() != null) g.setActive(r.active());
        return g;
    }

    @Transactional(readOnly = true)
    public List<Group> groupsOfUser(UUID userId) {
        return userGroups.findByUserId(userId).stream().map(m -> groups.findById(m.getGroupId()).orElse(null)).filter(Objects::nonNull).toList();
    }

    @Transactional(readOnly = true)
    public List<UserDto> membersOfGroup(UUID groupId) {
        getGroup(groupId);
        return userGroups.findByTargetId(groupId).stream().map(m -> users.findById(m.getUserId()).map(UserDto::from).orElse(null)).filter(Objects::nonNull).toList();
    }

    public void addUserToGroup(UUID groupId, UUID userId) {
        getGroup(groupId);
        if (!users.existsById(userId)) throw new NotFoundException("User not found: " + userId);
        userGroups.save(new UserGroup(userId, groupId));
    }

    public void removeUserFromGroup(UUID groupId, UUID userId) { userGroups.deleteById(new Membership.Key(userId, groupId)); }
}
