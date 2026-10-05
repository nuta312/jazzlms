package kz.jazz.lms.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import kz.jazz.lms.user.domain.UserType;

public record UpdateUserRequest(
        @Size(max = 50) String firstName,
        @Size(max = 50) String lastName,
        @Email String email,
        @Size(min = 6, max = 72) String password,
        UserType userType,
        String timeZone,
        String language,
        @Size(max = 800) String bio,
        Boolean active,
        Boolean excludeFromEmails
) {}
