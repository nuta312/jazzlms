package kz.jazz.lms.user.dto;

import kz.jazz.lms.user.domain.User;
import kz.jazz.lms.user.domain.UserType;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO (Data Transfer Object): то, что отдаём наружу через REST.
 * Сущность User наружу не отдаём — в ней passwordHash и внутренние детали.
 * Serializable нужен, т.к. объект кладётся в Redis-кэш.
 */
public record UserDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String username,
        UserType userType,
        String timeZone,
        String language,
        String bio,
        boolean active,
        boolean excludeFromEmails,
        Instant createdAt,
        Instant lastLoginAt
) implements Serializable {

    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(), u.getUsername(),
                u.getUserType(), u.getTimeZone(), u.getLanguage(), u.getBio(), u.isActive(),
                u.isExcludeFromEmails(), u.getCreatedAt(), u.getLastLoginAt());
    }
}
