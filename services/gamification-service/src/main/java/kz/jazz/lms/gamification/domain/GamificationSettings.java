package kz.jazz.lms.gamification.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Настройки геймификации (экран Account & Settings → Gamification), один документ в MongoDB.
 * MongoDB здесь удобна тем, что вложенную структуру (points / badges / levels / leaderboard)
 * не надо раскладывать по таблицам: документ выглядит ровно как JSON формы.
 */
@Document("settings")
public class GamificationSettings {
    public static final String ID = "default";

    /** Правило начисления: включено ли и сколько очков. */
    public record PointRule(boolean enabled, int points) {}
    public record Points(boolean enabled, PointRule login, PointRule unit, PointRule course, PointRule certificate, PointRule test) {}
    /** Категории бейджей (Activity = логины, Learning = уроки, Certification = курсы). */
    public record Badges(boolean enabled, boolean activity, boolean learning, boolean certification) {}
    /** "Upgrade level every N points / completed courses / badges" — уровень = максимум по включённым критериям. */
    public record LevelRule(boolean enabled, int every) {}
    public record Levels(boolean enabled, LevelRule byPoints, LevelRule byCourses, LevelRule byBadges) {}
    public record Leaderboard(boolean enabled, boolean showLevels, boolean showPoints, boolean showBadges) {}

    @Id
    private String id = ID;
    private boolean enabled = true;
    private Points points;
    private Badges badges;
    private Levels levels;
    private Leaderboard leaderboard;

    public static GamificationSettings defaults() {
        GamificationSettings s = new GamificationSettings();
        s.points = new Points(true, new PointRule(true, 10), new PointRule(true, 15), new PointRule(true, 100), new PointRule(true, 50), new PointRule(true, 20));
        s.badges = new Badges(true, true, true, true);
        s.levels = new Levels(true, new LevelRule(true, 300), new LevelRule(true, 5), new LevelRule(true, 5));
        s.leaderboard = new Leaderboard(true, true, true, true);
        return s;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Points getPoints() { return points; }
    public void setPoints(Points points) { this.points = points; }
    public Badges getBadges() { return badges; }
    public void setBadges(Badges badges) { this.badges = badges; }
    public Levels getLevels() { return levels; }
    public void setLevels(Levels levels) { this.levels = levels; }
    public Leaderboard getLeaderboard() { return leaderboard; }
    public void setLeaderboard(Leaderboard leaderboard) { this.leaderboard = leaderboard; }
}
