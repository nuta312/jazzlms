package kz.jazz.lms.user.service;

import kz.jazz.lms.events.EventType;
import kz.jazz.lms.user.domain.AccountSettings;
import kz.jazz.lms.user.domain.User;
import kz.jazz.lms.user.domain.UserType;
import kz.jazz.lms.user.dto.*;
import kz.jazz.lms.user.repository.UserRepository;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class AuthService {

    private final UserRepository repo;
    private final UserService userService;
    private final SettingsService settings;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final UserEventPublisher events;
    private final CacheManager cacheManager;
    private final StringRedisTemplate redis;

    public AuthService(UserRepository repo, UserService userService, SettingsService settings, PasswordEncoder encoder,
                       JwtService jwt, UserEventPublisher events, CacheManager cacheManager, StringRedisTemplate redis) {
        this.cacheManager = cacheManager;
        this.repo = repo;
        this.userService = userService;
        this.settings = settings;
        this.encoder = encoder;
        this.jwt = jwt;
        this.events = events;
        this.redis = redis;
    }

    /**
     * Вход. Защита от перебора паролей ("Lock account after N failed attempts for M minutes"):
     * счётчик неудач живёт в Redis под ключом login:fail:<login> с TTL = M минут (INCR + EXPIRE).
     * Redis здесь удобнее таблицы: TTL сам "разблокирует" аккаунт, а счётчик один на все инстансы сервиса.
     */
    public AuthResponse login(LoginRequest req) {
        AccountSettings s = settings.get();
        String login = req.username().trim().toLowerCase();
        String failKey = "login:fail:" + login;

        if (s.getLockAfterAttempts() != null) {
            String failed = redis.opsForValue().get(failKey);
            if (failed != null && Integer.parseInt(failed) >= s.getLockAfterAttempts()) {
                Long ttl = redis.getExpire(failKey, TimeUnit.SECONDS);
                long minutes = ttl == null || ttl <= 0 ? s.getLockMinutes() : Math.max(1, (ttl + 59) / 60);
                throw new LockedException("Account is locked after " + s.getLockAfterAttempts()
                        + " failed attempts. Try again in " + minutes + " minute(s)");
            }
        }

        User user = repo.findByUsername(req.username())
                .or(() -> repo.findByEmail(req.username()))
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> {
                    if (s.getLockAfterAttempts() != null) {
                        Long n = redis.opsForValue().increment(failKey);
                        redis.expire(failKey, Duration.ofMinutes(s.getLockMinutes()));
                        long left = s.getLockAfterAttempts() - (n == null ? 0 : n);
                        if (left <= 0) return new LockedException("Account is locked for " + s.getLockMinutes() + " minute(s)");
                        return new UnauthorizedException("Invalid username or password (" + left + " attempt(s) left)");
                    }
                    return new UnauthorizedException("Invalid username or password");
                });
        if (!user.isActive()) throw new UnauthorizedException("User is deactivated");
        redis.delete(failKey);   // успешный вход обнуляет счётчик

        user.setLastLoginAt(Instant.now());
        repo.save(user);
        evictCache(user.getId());   // lastLoginAt изменился — закэшированный UserDto устарел
        events.publish(EventType.USER_LOGGED_IN, user);
        return new AuthResponse(jwt.generate(user), UserDto.from(user), mustChangePassword(user, s));
    }

    /** "Enforce password change on first login" и "Enforce password change after N days". */
    private boolean mustChangePassword(User user, AccountSettings s) {
        if (user.isMustChangePassword()) return true;
        if (s.getPasswordMaxAgeDays() == null) return false;
        Instant changed = user.getPasswordChangedAt() == null ? user.getCreatedAt() : user.getPasswordChangedAt();
        return changed.plus(Duration.ofDays(s.getPasswordMaxAgeDays())).isBefore(Instant.now());
    }

    public void changePassword(UUID userId, ChangePasswordRequest req) {
        User user = userService.getEntity(userId);
        if (!encoder.matches(req.currentPassword(), user.getPasswordHash()))
            throw new UnauthorizedException("Current password is wrong");
        if (encoder.matches(req.newPassword(), user.getPasswordHash()))
            throw new BadRequestException("New password must differ from the current one");
        user.setPasswordHash(encoder.encode(req.newPassword()));
        user.setPasswordChangedAt(Instant.now());
        user.setMustChangePassword(false);
        repo.save(user);
        evictCache(userId);
    }

    /**
     * Саморегистрация. Настройки решают: разрешена ли она вообще (Signup: Direct / Manually),
     * какую роль получит новый пользователь (Default user type) и нужно ли принять Terms of Service.
     */
    public AuthResponse register(RegisterRequest req) {
        AccountSettings s = settings.get();
        if (s.getSignupMode() != AccountSettings.SignupMode.DIRECT)
            throw new ForbiddenException("Self-registration is disabled: ask an administrator to create your account");
        if (s.getTermsOfService() != null && !Boolean.TRUE.equals(req.acceptTerms()))
            throw new BadRequestException("You must accept the Terms of Service");
        UserType type = s.getDefaultUserType() == UserType.SUPER_ADMIN ? UserType.LEARNER : s.getDefaultUserType();
        UserDto dto = userService.create(new CreateUserRequest(req.firstName(), req.lastName(), req.email(),
                req.username(), req.password(), type, s.getDefaultTimeZone(), s.getDefaultLanguage(), null, true, false),
                EventType.USER_SIGNED_UP);
        User user = userService.getEntity(dto.id());
        return new AuthResponse(jwt.generate(user), dto);
    }

    /**
     * "Log into account": администратор получает токен ДРУГОГО пользователя, не зная его пароля.
     * Это мощная и опасная функция, поэтому: только админы (правило в gateway), нельзя войти под SuperAdmin,
     * в токен пишется impersonatedBy, а в Kafka уходит событие для аудита (видно в Timeline).
     */
    public AuthResponse impersonate(UUID adminId, UUID targetId) {
        User admin = userService.getEntity(adminId);
        User target = userService.getEntity(targetId);
        if (admin.getUserType() != UserType.SUPER_ADMIN && admin.getUserType() != UserType.ADMIN)
            throw new UnauthorizedException("Only administrators can log into other accounts");
        if (target.getUserType() == UserType.SUPER_ADMIN) throw new ConflictException("Cannot log into a SuperAdmin account");
        if (!target.isActive()) throw new ConflictException("User is deactivated");
        events.publishImpersonation(admin, target);
        return new AuthResponse(jwt.generate(target, adminId), UserDto.from(target));
    }

    private void evictCache(UUID userId) {
        var cache = cacheManager.getCache("users");
        if (cache != null) cache.evict(userId);
    }

    @Transactional(readOnly = true)
    public UserDto me(UUID userId) {
        return userService.findById(userId);
    }
}
