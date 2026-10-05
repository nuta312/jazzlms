package kz.jazz.lms.gamification.service;

import kz.jazz.lms.events.DomainEvent;
import kz.jazz.lms.gamification.domain.GamificationSettings;
import kz.jazz.lms.gamification.domain.Profile;
import kz.jazz.lms.gamification.domain.Rules;
import kz.jazz.lms.gamification.repository.ProcessedEventRepository;
import kz.jazz.lms.gamification.repository.ProfileRepository;
import kz.jazz.lms.gamification.domain.ProcessedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Начисление очков и бейджей по событиям из Kafka.
 * Redis ZSET хранит три рейтинга (points / levels / badges) — ZADD при каждом изменении, ZREVRANGE при чтении.
 */
@Service
public class GamificationService {
    private static final Logger log = LoggerFactory.getLogger(GamificationService.class);
    public static final String LB_POINTS = "leaderboard:points";
    public static final String LB_LEVELS = "leaderboard:levels";
    public static final String LB_BADGES = "leaderboard:badges";

    private final ProfileRepository profiles;
    private final ProcessedEventRepository processed;
    private final StringRedisTemplate redis;
    private final SettingsService settings;

    public GamificationService(ProfileRepository profiles, ProcessedEventRepository processed, StringRedisTemplate redis,
                               SettingsService settings) {
        this.profiles = profiles;
        this.processed = processed;
        this.redis = redis;
        this.settings = settings;
    }

    public void apply(DomainEvent event) {
        String userId = event.get("userId");
        if (userId == null || userId.isBlank()) return;
        GamificationSettings s = settings.get();
        if (!s.isEnabled()) return;                                          // "GAMIFICATION OFF" — ничего не начисляем
        var pts = s.getPoints();
        try {
            processed.insert(new ProcessedEvent(event.eventId().toString()));   // уникальный _id
        } catch (DuplicateKeyException e) {
            log.info("Event {} already processed, skipping", event.eventId());
            return;
        }

        Profile p = profiles.findById(userId).orElseGet(() -> { Profile n = new Profile(); n.setUserId(userId); return n; });
        switch (event.type()) {
            case USER_LOGGED_IN -> {
                String day = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC).toString();
                if (day.equals(p.getLastLoginDay())) return;          // сегодня уже начисляли
                p.setLastLoginDay(day);
                p.setLogins(p.getLogins() + 1);
                award(p, pts.enabled() && pts.login().enabled() ? pts.login().points() : 0, "Sign in", day);
            }
            case UNIT_COMPLETED -> {
                p.setUnits(p.getUnits() + 1);
                award(p, pts.enabled() && pts.unit().enabled() ? pts.unit().points() : 0, "Unit completed", event.get("unitName"));
            }
            case COURSE_COMPLETED -> {
                p.setCourses(p.getCourses() + 1);
                award(p, pts.enabled() && pts.course().enabled() ? pts.course().points() : 0, "Course completed", event.get("courseName"));
            }
            case TEST_COMPLETED -> {
                if (!Boolean.parseBoolean(event.get("passed"))) return;      // очки только за сданный тест
                award(p, pts.enabled() && pts.test().enabled() ? pts.test().points() : 0, "Test passed", event.get("unitName") + " (" + event.get("score") + "%)");
            }
            case CERTIFICATE_ISSUED -> award(p, pts.enabled() && pts.certificate().enabled() ? pts.certificate().points() : 0,
                    "Certificate", event.get("courseName"));
            default -> { return; }
        }
        checkBadges(p, s);
        p.setLevel(Rules.levelFor(p.getPoints(), p.getCourses(), p.getBadges().size(), s));
        p.setUpdatedAt(Instant.now());
        profiles.save(p);

        redis.opsForZSet().add(LB_POINTS, userId, p.getPoints());
        redis.opsForZSet().add(LB_LEVELS, userId, p.getLevel());
        redis.opsForZSet().add(LB_BADGES, userId, p.getBadges().size());
        log.info("{}: {} -> {} points, level {}, {} badges", userId, event.type(), p.getPoints(), p.getLevel(), p.getBadges().size());
    }

    private void award(Profile p, int points, String action, String note) {
        p.setPoints(p.getPoints() + points);
        p.getHistory().add(0, new Profile.HistoryEntry(Instant.now(), action, points, note));
        if (p.getHistory().size() > 50) p.getHistory().subList(50, p.getHistory().size()).clear();
    }

    private void checkBadges(Profile p, GamificationSettings s) {
        for (Rules.BadgeDef def : Rules.BADGES) {
            if (!Rules.badgeEnabled(def, s)) continue;
            boolean has = p.getBadges().stream().anyMatch(b -> b.code().equals(def.code()));
            if (has) continue;
            int counter = switch (def.counter()) { case "logins" -> p.getLogins(); case "units" -> p.getUnits(); default -> p.getCourses(); };
            if (counter >= def.threshold()) {
                p.getBadges().add(new Profile.Badge(def.code(), def.name(), def.category(), def.icon(), Instant.now()));
                p.getHistory().add(0, new Profile.HistoryEntry(Instant.now(), "Badge: " + def.name(), 0, def.category()));
            }
        }
    }
}
