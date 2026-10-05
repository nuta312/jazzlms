package kz.jazz.lms.gamification.service;

import kz.jazz.lms.gamification.domain.GamificationSettings;
import kz.jazz.lms.gamification.domain.Rules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Уровни считаются по настройкам ("Upgrade level every N points / courses / badges"). */
class RulesTest {
    private final GamificationSettings s = GamificationSettings.defaults();   // 300 очков, 5 курсов, 5 бейджей

    @Test
    void levelThresholdsFollowSettings() {
        assertEquals(0, Rules.pointsForLevel(1, s));
        assertEquals(300, Rules.pointsForLevel(2, s));
        assertEquals(1200, Rules.pointsForLevel(5, s));
    }

    @Test
    void levelIsMaxOfEnabledCriteria() {
        assertEquals(1, Rules.levelFor(0, 0, 0, s));
        assertEquals(1, Rules.levelFor(299, 0, 0, s));
        assertEquals(2, Rules.levelFor(300, 0, 0, s));
        assertEquals(3, Rules.levelFor(0, 10, 0, s));      // 10 курсов / 5 = +2 уровня
        assertEquals(2, Rules.levelFor(0, 0, 5, s));
        assertEquals(Rules.MAX_LEVEL, Rules.levelFor(1_000_000, 0, 0, s));
    }

    @Test
    void disabledLevelsAlwaysOne() {
        s.setLevels(new GamificationSettings.Levels(false, new GamificationSettings.LevelRule(true, 300),
                new GamificationSettings.LevelRule(true, 5), new GamificationSettings.LevelRule(true, 5)));
        assertEquals(1, Rules.levelFor(5000, 50, 50, s));
        assertNull(Rules.pointsForLevel(2, s));
    }

    @Test
    void badgeCategoriesCanBeSwitchedOff() {
        s.setBadges(new GamificationSettings.Badges(true, false, true, true));
        assertFalse(Rules.badgeEnabled(Rules.BADGES.get(0), s));   // Activity выключена
        assertTrue(Rules.badgeEnabled(Rules.BADGES.get(3), s));    // Learning включена
    }
}
