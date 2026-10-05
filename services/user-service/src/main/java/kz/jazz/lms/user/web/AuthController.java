package kz.jazz.lms.user.web;

import jakarta.validation.Valid;
import kz.jazz.lms.user.dto.*;
import kz.jazz.lms.user.service.AuthService;
import kz.jazz.lms.user.service.SettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Публичные эндпоинты (gateway пропускает /api/auth/** без токена).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final SettingsService settings;

    public AuthController(AuthService auth, SettingsService settings) { this.auth = auth; this.settings = settings; }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    /** Настройки, нужные ещё до входа: разрешён ли Sign up, текст Terms of Service. Публичный путь. */
    @GetMapping("/settings")
    public PublicSettingsDto settings() { return PublicSettingsDto.from(settings.get()); }

    /** Смена своего пароля (в т.ч. принудительная при первом входе). */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest req, @RequestHeader("X-User-Id") UUID userId) {
        auth.changePassword(userId, req);
    }

    /** Log into account. Путь закрыт в gateway для всех, кроме ADMIN / SUPER_ADMIN. */
    @PostMapping("/impersonate/{userId}")
    public AuthResponse impersonate(@PathVariable UUID userId, @RequestHeader("X-User-Id") UUID adminId) {
        return auth.impersonate(adminId, userId);
    }

    /** Gateway проверил JWT и положил id пользователя в заголовок X-User-Id. */
    @GetMapping("/me")
    public UserDto me(@RequestHeader("X-User-Id") UUID userId) {
        return auth.me(userId);
    }
}
