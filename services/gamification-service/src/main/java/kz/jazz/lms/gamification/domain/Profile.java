package kz.jazz.lms.gamification.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Игровой профиль пользователя. Один документ на пользователя, id = userId из user-service. */
@Document("profiles")
public class Profile {
    public record Badge(String code, String name, String category, String icon, Instant awardedAt) {}
    public record HistoryEntry(Instant at, String action, int points, String note) {}

    @Id
    private String userId;
    private int points;
    private int level = 1;
    private int logins;          // счётчики для бейджей
    private int units;
    private int courses;
    private String lastLoginDay; // очки за вход — раз в день
    private List<Badge> badges = new ArrayList<>();
    private List<HistoryEntry> history = new ArrayList<>();   // последние 50 начислений
    private Instant updatedAt = Instant.now();

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getLogins() { return logins; }
    public void setLogins(int logins) { this.logins = logins; }
    public int getUnits() { return units; }
    public void setUnits(int units) { this.units = units; }
    public int getCourses() { return courses; }
    public void setCourses(int courses) { this.courses = courses; }
    public String getLastLoginDay() { return lastLoginDay; }
    public void setLastLoginDay(String lastLoginDay) { this.lastLoginDay = lastLoginDay; }
    public List<Badge> getBadges() { return badges; }
    public List<HistoryEntry> getHistory() { return history; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
