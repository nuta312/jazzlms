package kz.jazz.lms.user.dto;

import jakarta.validation.constraints.*;
import kz.jazz.lms.user.domain.Branch;
import kz.jazz.lms.user.domain.UserType;

public record BranchRequest(
        @NotBlank @Size(max = 60) @Pattern(regexp = "[a-z0-9-]+", message = "only lowercase letters, digits and dashes") String name,
        @Size(max = 120) String title,
        @Size(max = 255) String description,
        String language,
        String timeZone,
        UserType defaultUserType,
        Branch.SignupMode signupMode,
        @Size(max = 2000) String announcement,
        Boolean active
) {}
