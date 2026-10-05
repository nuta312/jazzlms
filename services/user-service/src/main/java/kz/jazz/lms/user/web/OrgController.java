package kz.jazz.lms.user.web;

import jakarta.validation.Valid;
import kz.jazz.lms.user.domain.Branch;
import kz.jazz.lms.user.domain.Group;
import kz.jazz.lms.user.dto.BranchRequest;
import kz.jazz.lms.user.dto.GroupRequest;
import kz.jazz.lms.user.dto.UserDto;
import kz.jazz.lms.user.service.OrgService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ветки и группы. Чтение — staff, изменения — только администратор (правила в gateway).
 * Вложенные ресурсы: /api/branches/{id}/users — участники ветки; /api/users/{id}/branches — ветки пользователя.
 */
@RestController
public class OrgController {
    private final OrgService org;

    public OrgController(OrgService org) { this.org = org; }

    // --- branches ---
    @GetMapping("/api/branches")
    public List<Map<String, Object>> branches() {
        return org.allBranches().stream().map(v -> Map.<String, Object>of("branch", v.branch(), "members", v.members())).toList();
    }

    @GetMapping("/api/branches/{id}")
    public Branch branch(@PathVariable UUID id) { return org.getBranch(id); }

    @PostMapping("/api/branches")
    @ResponseStatus(HttpStatus.CREATED)
    public Branch createBranch(@Valid @RequestBody BranchRequest req) { return org.createBranch(req); }

    @PutMapping("/api/branches/{id}")
    public Branch updateBranch(@PathVariable UUID id, @Valid @RequestBody BranchRequest req) { return org.updateBranch(id, req); }

    @DeleteMapping("/api/branches/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBranch(@PathVariable UUID id) { org.deleteBranch(id); }

    @GetMapping("/api/branches/{id}/users")
    public List<UserDto> branchMembers(@PathVariable UUID id) { return org.membersOfBranch(id); }

    @PostMapping("/api/branches/{id}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addToBranch(@PathVariable UUID id, @PathVariable UUID userId) { org.addUserToBranch(id, userId); }

    @DeleteMapping("/api/branches/{id}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromBranch(@PathVariable UUID id, @PathVariable UUID userId) { org.removeUserFromBranch(id, userId); }

    @GetMapping("/api/users/{id}/branches")
    public List<Branch> userBranches(@PathVariable UUID id) { return org.branchesOfUser(id); }

    // --- groups ---
    @GetMapping("/api/groups")
    public List<Map<String, Object>> groups() {
        return org.allGroups().stream().map(v -> Map.<String, Object>of("group", v.group(), "members", v.members())).toList();
    }

    @GetMapping("/api/groups/{id}")
    public Group group(@PathVariable UUID id) { return org.getGroup(id); }

    @PostMapping("/api/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public Group createGroup(@Valid @RequestBody GroupRequest req) { return org.createGroup(req); }

    @PutMapping("/api/groups/{id}")
    public Group updateGroup(@PathVariable UUID id, @Valid @RequestBody GroupRequest req) { return org.updateGroup(id, req); }

    @DeleteMapping("/api/groups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable UUID id) { org.deleteGroup(id); }

    @GetMapping("/api/groups/{id}/users")
    public List<UserDto> groupMembers(@PathVariable UUID id) { return org.membersOfGroup(id); }

    @PostMapping("/api/groups/{id}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addToGroup(@PathVariable UUID id, @PathVariable UUID userId) { org.addUserToGroup(id, userId); }

    @DeleteMapping("/api/groups/{id}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromGroup(@PathVariable UUID id, @PathVariable UUID userId) { org.removeUserFromGroup(id, userId); }

    @GetMapping("/api/users/{id}/groups")
    public List<Group> userGroups(@PathVariable UUID id) { return org.groupsOfUser(id); }
}
