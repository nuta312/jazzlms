package kz.jazz.lms.gamification.service;

import kz.jazz.lms.gamification.domain.GamificationSettings;
import kz.jazz.lms.gamification.repository.GamificationSettingsRepository;
import kz.jazz.lms.gamification.repository.ProcessedEventRepository;
import kz.jazz.lms.gamification.repository.ProfileRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettingsService {
    private final GamificationSettingsRepository repo;
    private final ProfileRepository profiles;
    private final ProcessedEventRepository processed;
    private final StringRedisTemplate redis;

    public SettingsService(GamificationSettingsRepository repo, ProfileRepository profiles,
                           ProcessedEventRepository processed, StringRedisTemplate redis) {
        this.repo = repo; this.profiles = profiles; this.processed = processed; this.redis = redis;
    }

    /** Нет документа — значит, ещё ни разу не сохраняли: отдаём значения по умолчанию. */
    public GamificationSettings get() {
        GamificationSettings s = repo.findById(GamificationSettings.ID).orElseGet(GamificationSettings::defaults);
        // документ мог быть сохранён до появления правила "test" — дополняем значением по умолчанию
        if (s.getPoints() != null && s.getPoints().test() == null) {
            var p = s.getPoints();
            s.setPoints(new GamificationSettings.Points(p.enabled(), p.login(), p.unit(), p.course(), p.certificate(), GamificationSettings.defaults().getPoints().test()));
        }
        return s;
    }

    public GamificationSettings update(GamificationSettings s) {
        GamificationSettings d = GamificationSettings.defaults();
        s.setId(GamificationSettings.ID);
        if (s.getPoints() == null) s.setPoints(d.getPoints());
        else if (s.getPoints().test() == null) { var p = s.getPoints(); s.setPoints(new GamificationSettings.Points(p.enabled(), p.login(), p.unit(), p.course(), p.certificate(), d.getPoints().test())); }
        if (s.getBadges() == null) s.setBadges(d.getBadges());
        if (s.getLevels() == null) s.setLevels(d.getLevels());
        if (s.getLeaderboard() == null) s.setLeaderboard(d.getLeaderboard());
        return repo.save(s);
    }

    /** "Reset to default settings". */
    public GamificationSettings reset() { return repo.save(GamificationSettings.defaults()); }

    /**
     * "Reset statistics": стираем все профили, рейтинги в Redis и журнал обработанных событий.
     * Журнал тоже стираем, иначе при повторной доставке события Kafka оно было бы пропущено как "уже учтённое".
     */
    public void resetStatistics() {
        profiles.deleteAll();
        processed.deleteAll();
        redis.delete(List.of(GamificationService.LB_POINTS, GamificationService.LB_LEVELS, GamificationService.LB_BADGES));
    }
}
