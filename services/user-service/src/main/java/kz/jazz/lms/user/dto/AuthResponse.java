package kz.jazz.lms.user.dto;

/** mustChangePassword = true: фронт сразу ведёт на смену пароля (первый вход или пароль устарел). */
public record AuthResponse(String token, UserDto user, boolean mustChangePassword) {
    public AuthResponse(String token, UserDto user) { this(token, user, false); }
}
