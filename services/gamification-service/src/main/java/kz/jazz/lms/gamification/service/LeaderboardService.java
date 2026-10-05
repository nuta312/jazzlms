package kz.jazz.lms.gamification.service;

import kz.jazz.lms.gamification.client.UserClient;
import kz.jazz.lms.gamification.domain.GamificationSettings;
import kz.jazz.lms.gamification.domain.Profile;
import kz.jazz.lms.gamification.domain.Rules;
import kz.jazz.lms.gamification.repository.ProfileRepository;
import kz.jazz.lms.grpc.user.UserResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LeaderboardService {
    public record Entry(int rank, String userId, String name, long score, boolean me) {}
    public record Board(String by, List<Entry> top, Entry me) {}

    private final StringRedisTemplate redis;
    private final UserClient users;
    private final ProfileRepository profiles;
    private final SettingsService settings;

    public LeaderboardService(StringRedisTemplate redis, UserClient users, ProfileRepository profiles, SettingsService settings) {
        this.redis = redis;
        this.users = users;
        this.profiles = profiles;
        this.settings = settings;
    }

    /** by = points | levels | badges. Топ-N плюс строка текущего пользователя, если он ниже (как в TalentLMS). */
    public Board board(String by, String meId, int limit) {
        String key = switch (by) { case "levels" -> GamificationService.LB_LEVELS; case "badges" -> GamificationService.LB_BADGES; default -> GamificationService.LB_POINTS; };
        Set<ZSetOperations.TypedTuple<String>> top = redis.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);
        List<String> ids = new ArrayList<>();
        if (top != null) top.forEach(t -> ids.add(t.getValue()));
        Long myRank = meId == null ? null : redis.opsForZSet().reverseRank(key, meId);
        Double myScore = meId == null ? null : redis.opsForZSet().score(key, meId);
        if (myRank != null && !ids.contains(meId)) ids.add(meId);

        Map<String, UserResponse> names = users.getUsers(ids);   // один gRPC-вызов на весь список
        List<Entry> entries = new ArrayList<>();
        int rank = 1;
        if (top != null) for (var t : top) {
            entries.add(new Entry(rank++, t.getValue(), name(names, t.getValue()), Math.round(t.getScore() == null ? 0 : t.getScore()), t.getValue().equals(meId)));
        }
        Entry me = myRank == null ? null : new Entry((int) (myRank + 1), meId, name(names, meId), Math.round(myScore == null ? 0 : myScore), true);
        return new Board(by, entries, me);
    }

    public Map<String, Object> profile(String userId) {
        Profile p = profiles.findById(userId).orElseGet(() -> { Profile n = new Profile(); n.setUserId(userId); return n; });
        GamificationSettings s = settings.get();
        int next = Math.min(Rules.MAX_LEVEL, p.getLevel() + 1);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", userId);
        m.put("enabled", s.isEnabled());
        m.put("points", p.getPoints());
        m.put("level", p.getLevel());
        m.put("nextLevelPoints", p.getLevel() >= Rules.MAX_LEVEL ? null : Rules.pointsForLevel(next, s));
        m.put("logins", p.getLogins());
        m.put("units", p.getUnits());
        m.put("courses", p.getCourses());
        m.put("badges", p.getBadges());
        m.put("history", p.getHistory());
        Long rank = redis.opsForZSet().reverseRank(GamificationService.LB_POINTS, userId);
        m.put("rank", rank == null ? null : rank + 1);
        Long lvlRank = redis.opsForZSet().reverseRank(GamificationService.LB_LEVELS, userId);
        Long bdgRank = redis.opsForZSet().reverseRank(GamificationService.LB_BADGES, userId);
        m.put("levelRank", lvlRank == null ? null : lvlRank + 1);      // "Compared to others"
        m.put("badgeRank", bdgRank == null ? null : bdgRank + 1);
        return m;
    }

    /** "How to collect points": правила, пороги уровней и все бейджи с отметкой "получен". */
    public Map<String, Object> rules(String userId) {
        Set<String> mine = new HashSet<>();
        GamificationSettings s = settings.get();
        profiles.findById(userId).ifPresent(p -> p.getBadges().forEach(b -> mine.add(b.code())));
        List<Map<String, Object>> levels = new ArrayList<>();
        for (int l = 1; l <= Rules.MAX_LEVEL; l++) {
            Integer pts = Rules.pointsForLevel(l, s);
            if (pts != null) levels.add(Map.of("level", l, "points", pts));
        }
        List<Map<String, Object>> badges = new ArrayList<>();
        for (Rules.BadgeDef d : Rules.BADGES) {
            if (!Rules.badgeEnabled(d, s)) continue;
            badges.add(Map.of("code", d.code(), "name", d.name(), "category", d.category(),
                    "icon", d.icon(), "threshold", d.threshold(), "counter", d.counter(), "earned", mine.contains(d.code())));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("points", Rules.pointRules(s));
        m.put("levels", levels);
        m.put("levelRules", s.getLevels());
        m.put("badges", badges);
        return m;
    }

    private static String name(Map<String, UserResponse> names, String id) {
        UserResponse u = names.get(id);
        return u == null ? "Unknown user" : (u.getFirstName() + " " + u.getLastName()).trim();
    }
}
