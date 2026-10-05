package kz.jazz.lms.gamification.domain;

import java.util.List;

/**
 * Правила игры. Очки и пороги уровней теперь берутся из {@link GamificationSettings}
 * (экран Account & Settings → Gamification); здесь остались только константы и формулы.
 */
public final class Rules {
    private Rules() {}

    public static final int MAX_LEVEL = 20;

    public record PointRule(String action, int points, String note) {}

    /** Категории и пороги, как у TalentLMS: Activity (логины), Learning (уроки), Certification (курсы). */
    public record BadgeDef(String code, String name, String category, String counter, int threshold, String icon) {}

    public static final List<BadgeDef> BADGES = List.of(
            new BadgeDef("newcomer", "Newcomer", "Activity", "logins", 1, "🌱"),
            new BadgeDef("regular", "Regular", "Activity", "logins", 5, "🔥"),
            new BadgeDef("veteran", "Veteran", "Activity", "logins", 25, "🏅"),
            new BadgeDef("first-step", "First step", "Learning", "units", 1, "👣"),
            new BadgeDef("learner", "Learner", "Learning", "units", 10, "📚"),
            new BadgeDef("scholar", "Scholar", "Learning", "units", 50, "🎓"),
            new BadgeDef("graduate", "Graduate", "Certification", "courses", 1, "🏆"),
            new BadgeDef("master", "Master", "Certification", "courses", 5, "👑"));

    /** Включена ли категория бейджа в настройках. */
    public static boolean badgeEnabled(BadgeDef def, GamificationSettings s) {
        if (!s.getBadges().enabled()) return false;
        return switch (def.category()) {
            case "Activity" -> s.getBadges().activity();
            case "Learning" -> s.getBadges().learning();
            default -> s.getBadges().certification();
        };
    }

    /** Список действий с очками для окна "How to collect points". */
    public static List<PointRule> pointRules(GamificationSettings s) {
        var p = s.getPoints();
        java.util.ArrayList<PointRule> list = new java.util.ArrayList<>();
        if (p.login().enabled()) list.add(new PointRule("Sign in", p.login().points(), "once per day"));
        if (p.unit().enabled()) list.add(new PointRule("Complete a unit", p.unit().points(), ""));
        if (p.course().enabled()) list.add(new PointRule("Complete a course", p.course().points(), ""));
        if (p.certificate().enabled()) list.add(new PointRule("Get a certificate", p.certificate().points(), ""));
        if (p.test() != null && p.test().enabled()) list.add(new PointRule("Pass a test", p.test().points(), ""));
        return list;
    }

    /**
     * Уровень = 1 + max(points / everyPoints, courses / everyCourses, badges / everyBadges)
     * по включённым критериям, не больше MAX_LEVEL. Как в TalentLMS: "Upgrade level every ...".
     */
    public static int levelFor(int points, int courses, int badges, GamificationSettings s) {
        var l = s.getLevels();
        if (!l.enabled()) return 1;
        int level = 1;
        if (l.byPoints().enabled() && l.byPoints().every() > 0) level = Math.max(level, 1 + points / l.byPoints().every());
        if (l.byCourses().enabled() && l.byCourses().every() > 0) level = Math.max(level, 1 + courses / l.byCourses().every());
        if (l.byBadges().enabled() && l.byBadges().every() > 0) level = Math.max(level, 1 + badges / l.byBadges().every());
        return Math.min(MAX_LEVEL, level);
    }

    /** Сколько очков нужно для уровня (по критерию "очки"); null, если критерий выключен. */
    public static Integer pointsForLevel(int level, GamificationSettings s) {
        var l = s.getLevels();
        if (!l.enabled() || !l.byPoints().enabled()) return null;
        return (level - 1) * l.byPoints().every();
    }
}
