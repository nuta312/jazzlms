package kz.jazz.lms.user.service;

import kz.jazz.lms.events.EventType;
import kz.jazz.lms.user.domain.User;
import kz.jazz.lms.user.domain.UserType;
import kz.jazz.lms.user.dto.CreateUserRequest;
import kz.jazz.lms.user.dto.UpdateUserRequest;
import kz.jazz.lms.user.dto.UserDto;
import kz.jazz.lms.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Бизнес-логика работы с пользователями.
 * Контроллер -> сервис -> репозиторий -> БД. Контроллер не знает про SQL,
 * репозиторий не знает про HTTP.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final UserEventPublisher events;
    private final SettingsService settings;
    private final OrgService org;

    public UserService(UserRepository repo, PasswordEncoder encoder, UserEventPublisher events,
                       SettingsService settings, OrgService org) {
        this.repo = repo;
        this.encoder = encoder;
        this.events = events;
        this.settings = settings;
        this.org = org;
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAll(String search, Boolean active) {
        String pattern = (search == null || search.isBlank()) ? "%" : "%" + search.trim().toLowerCase() + "%";
        return repo.search(pattern, active != null, active != null && active).stream().map(UserDto::from).toList();
    }

    /** Цифры для таблицы Overview на главной администратора. */
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> stats() {
        return java.util.Map.of("total", repo.count(), "active", repo.countByActiveTrue());
    }

    /** "Save as CSV" — те же поиск и фильтр, что на экране. */
    @Transactional(readOnly = true)
    public String exportCsv(String search, Boolean active) {
        StringBuilder sb = new StringBuilder("firstName,lastName,email,username,userType,active,registration,lastLogin\n");
        for (UserDto u : findAll(search, active)) {
            sb.append(String.join(",", Csv.escape(u.firstName()), Csv.escape(u.lastName()), Csv.escape(u.email()),
                    Csv.escape(u.username()), Csv.escape(u.userType()), Csv.escape(u.active()),
                    Csv.escape(u.createdAt()), Csv.escape(u.lastLoginAt()))).append('\n');
        }
        return sb.toString();
    }

    /**
     * Первый вызов идёт в PostgreSQL, результат кладётся в Redis под ключом users::<id>.
     * Последующие вызовы (в т.ч. gRPC от course-service) читают из Redis.
     */
    @Cacheable(value = "users", key = "#id")
    @Transactional(readOnly = true)
    public UserDto findById(UUID id) {
        return UserDto.from(getEntity(id));
    }

    public UserDto create(CreateUserRequest req, EventType eventType) {
        if (repo.existsByUsername(req.username())) throw new ConflictException("Username already taken: " + req.username());
        if (repo.existsByEmail(req.email())) throw new ConflictException("Email already registered: " + req.email());

        User u = new User();
        u.setFirstName(req.firstName());
        u.setLastName(req.lastName());
        u.setEmail(req.email());
        u.setUsername(req.username());
        String rawPassword = (req.password() == null || req.password().isBlank())
                ? UUID.randomUUID().toString().substring(0, 10)   // "Blank for random password"
                : req.password();
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setUserType(req.userType() == null ? UserType.LEARNER : req.userType());
        if (req.timeZone() != null) u.setTimeZone(req.timeZone());
        if (req.language() != null) u.setLanguage(req.language());
        u.setBio(req.bio());
        if (req.active() != null) u.setActive(req.active());
        if (req.excludeFromEmails() != null) u.setExcludeFromEmails(req.excludeFromEmails());

        // Account & Settings → Users: пользователю, которого завёл админ, можно предписать смену пароля при первом входе
        var s = settings.get();
        if (eventType == EventType.USER_CREATED && s.isPasswordChangeOnFirstLogin()) u.setMustChangePassword(true);
        u.setPasswordChangedAt(Instant.now());

        User saved = repo.save(u);
        if (s.getDefaultGroupId() != null) org.addUserToGroup(s.getDefaultGroupId(), saved.getId());   // Default group
        events.publish(eventType, saved);
        return UserDto.from(saved);
    }

    @CacheEvict(value = "users", key = "#id")   // данные изменились — выкидываем из кэша
    public UserDto update(UUID id, UpdateUserRequest req) {
        User u = getEntity(id);
        if (req.firstName() != null) u.setFirstName(req.firstName());
        if (req.lastName() != null) u.setLastName(req.lastName());
        if (req.email() != null && !req.email().equals(u.getEmail())) {
            if (repo.existsByEmail(req.email())) throw new ConflictException("Email already registered: " + req.email());
            u.setEmail(req.email());
        }
        if (req.password() != null && !req.password().isBlank()) {
            u.setPasswordHash(encoder.encode(req.password()));
            u.setPasswordChangedAt(Instant.now());
        }
        if (req.userType() != null) u.setUserType(req.userType());
        if (req.timeZone() != null) u.setTimeZone(req.timeZone());
        if (req.language() != null) u.setLanguage(req.language());
        if (req.bio() != null) u.setBio(req.bio());
        if (req.active() != null) u.setActive(req.active());
        if (req.excludeFromEmails() != null) u.setExcludeFromEmails(req.excludeFromEmails());
        return UserDto.from(repo.save(u));
    }

    @CacheEvict(value = "users", key = "#id")
    public void delete(UUID id) {
        User u = getEntity(id);
        if (u.getUserType() == UserType.SUPER_ADMIN) throw new ConflictException("Cannot delete SuperAdmin");
        repo.delete(u);
        events.publish(EventType.USER_DELETED, u);
    }

    User getEntity(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
    }
}
