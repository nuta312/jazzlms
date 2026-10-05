package kz.jazz.lms.gamification.web;

import kz.jazz.lms.gamification.domain.GamificationSettings;
import kz.jazz.lms.gamification.service.LeaderboardService;
import kz.jazz.lms.gamification.service.SettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/gamification")
public class GamificationController {
    private final LeaderboardService leaderboard;
    private final SettingsService settings;

    public GamificationController(LeaderboardService leaderboard, SettingsService settings) {
        this.leaderboard = leaderboard;
        this.settings = settings;
    }

    // --- Account & Settings → Gamification (админ, правило в gateway) ---
    @GetMapping("/settings")
    public GamificationSettings getSettings() { return settings.get(); }

    @PutMapping("/settings")
    public GamificationSettings updateSettings(@RequestBody GamificationSettings s) { return settings.update(s); }

    @PostMapping("/settings/reset")
    public GamificationSettings resetSettings() { return settings.reset(); }

    @PostMapping("/reset-statistics")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetStatistics() { settings.resetStatistics(); }

    /** Что показывать в интерфейсе: включена ли игра, какие вкладки у таблицы лидеров. Доступно всем ролям. */
    @GetMapping("/config")
    public Map<String, Object> config() {
        GamificationSettings s = settings.get();
        return Map.of("enabled", s.isEnabled(), "points", s.getPoints().enabled(), "badges", s.getBadges().enabled(),
                "levels", s.getLevels().enabled(), "leaderboard", s.getLeaderboard());
    }

    /** Мои очки, уровень, бейджи, история начислений. */
    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader("X-User-Id") UUID userId) { return leaderboard.profile(userId.toString()); }

    /** Профиль любого пользователя — для отчёта Users -> Reports (staff, правило в gateway). */
    @GetMapping("/users/{id}")
    public Map<String, Object> user(@PathVariable UUID id) { return leaderboard.profile(id.toString()); }

    /** GET /api/gamification/leaderboard?by=points|levels|badges */
    @GetMapping("/leaderboard")
    public LeaderboardService.Board leaderboard(@RequestParam(defaultValue = "points") String by,
                                                @RequestParam(defaultValue = "6") int limit,
                                                @RequestHeader("X-User-Id") UUID userId) {
        return leaderboard.board(by, userId.toString(), Math.min(limit, 50));
    }

    /** "How to collect points" / "How to level up". */
    @GetMapping("/rules")
    public Map<String, Object> rules(@RequestHeader("X-User-Id") UUID userId) { return leaderboard.rules(userId.toString()); }
}
