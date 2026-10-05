package kz.jazz.lms.analytics.service;

import kz.jazz.lms.analytics.domain.ActivityEvent;
import kz.jazz.lms.analytics.repository.ActivityEventRepository;
import kz.jazz.lms.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Redis используется как быстрое хранилище счётчиков:
 *   HINCRBY stats:logins:2026-09-16 14 1   -> +1 логин в 14:00 16 сентября
 *   ZINCRBY leaderboard:active-users 1 <userId>  -> рейтинг самых активных
 * MongoDB хранит полную ленту событий для таймлайна.
 */
@Service
public class StatsService {
    private static final Logger log = LoggerFactory.getLogger(StatsService.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String LEADERBOARD = "leaderboard:active-users";

    private final StringRedisTemplate redis;
    private final ActivityEventRepository events;
    private final MongoTemplate mongo;

    public StatsService(StringRedisTemplate redis, ActivityEventRepository events, MongoTemplate mongo) {
        this.mongo = mongo;
        this.redis = redis;
        this.events = events;
    }

    // ---------- запись ----------

    public void record(DomainEvent event) {
        if (events.existsByEventId(event.eventId().toString())) {
            log.info("Duplicate event {} ignored (idempotency)", event.eventId());
            return;
        }
        ActivityEvent a = new ActivityEvent();
        a.setEventId(event.eventId().toString());
        a.setType(event.type());
        a.setSource(event.source());
        a.setOccurredAt(event.occurredAt());
        a.setUserId(event.get("userId"));
        a.setPayload(event.payload());
        a.setDescription(describe(event));
        events.save(a);

        String metric = switch (event.type()) {
            case USER_LOGGED_IN -> "logins";
            case COURSE_COMPLETED -> "completions";
            case USER_ENROLLED -> "enrollments";
            case USER_CREATED, USER_SIGNED_UP -> "signups";
            default -> null;
        };
        if (metric != null) {
            LocalDateTime t = LocalDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC);
            String key = "stats:" + metric + ":" + t.toLocalDate().format(DAY);
            redis.opsForHash().increment(key, String.valueOf(t.getHour()), 1);
            redis.expire(key, Duration.ofDays(60));
        }
        if (event.get("userId") != null) {
            redis.opsForZSet().incrementScore(LEADERBOARD, event.get("userId"), 1);
        }
    }

    private static String describe(DomainEvent e) {
        String who = (e.get("firstName") == null ? "" : e.get("firstName") + " " + e.get("lastName")).trim();
        return switch (e.type()) {
            case USER_LOGGED_IN -> who + " signed in";
            case USER_CREATED -> who + " was added by an administrator";
            case USER_SIGNED_UP -> who + " signed up";
            case COURSE_CREATED -> "Course \"" + e.get("courseName") + "\" was created";
            case COURSE_UPDATED -> "Course \"" + e.get("courseName") + "\" was updated";
            case USER_ENROLLED -> who + " was enrolled in \"" + e.get("courseName") + "\"";
            case COURSE_COMPLETED -> who + " completed \"" + e.get("courseName") + "\"";
            case ASSIGNMENT_SUBMITTED -> who + " submitted an assignment";
            case COURSE_DELETED -> "Course \"" + e.get("courseName") + "\" was deleted";
            case COURSE_RESTORED -> "Course \"" + e.get("courseName") + "\" was restored";
            case USER_IMPERSONATED -> who + " logged into the account of " + e.get("targetName");
            case USER_DELETED -> who + " was deleted";
            case UNIT_ADDED -> who + " added unit \"" + e.get("unitName") + "\" (" + e.get("unitType") + ") to \"" + e.get("courseName") + "\"";
            case UNIT_COMPLETED -> "Unit \"" + e.get("unitName") + "\" of \"" + e.get("courseName") + "\" completed (" + e.get("progress") + "%)";
            case CERTIFICATE_ISSUED -> who + " received a certificate for \"" + e.get("courseName") + "\"";
            case TEST_COMPLETED -> who + (Boolean.parseBoolean(e.get("passed")) ? " passed" : " failed") + " the test \"" + e.get("unitName") + "\" with " + e.get("score") + "%";
        };
    }

    // ---------- чтение ----------

    /**
     * Серии для графика на главной (как в TalentLMS: Today / Yesterday / Week / Month).
     * today/yesterday -> 24 точки по часам; week/month -> по дням.
     */
    public Map<String, Object> dashboard(String period) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<String> labels = new ArrayList<>();
        List<Long> logins = new ArrayList<>();
        List<Long> completions = new ArrayList<>();

        switch (period == null ? "today" : period) {
            case "yesterday" -> hourly(today.minusDays(1), labels, logins, completions);
            case "week" -> daily(today.minusDays(6), today, labels, logins, completions);
            case "month" -> daily(today.minusDays(29), today, labels, logins, completions);
            default -> hourly(today, labels, logins, completions);
        }
        return Map.of("period", period == null ? "today" : period,
                "labels", labels, "logins", logins, "completions", completions,
                "totals", Map.of(
                        "logins", logins.stream().mapToLong(Long::longValue).sum(),
                        "completions", completions.stream().mapToLong(Long::longValue).sum()));
    }

    private void hourly(LocalDate day, List<String> labels, List<Long> logins, List<Long> completions) {
        Map<Object, Object> l = redis.opsForHash().entries("stats:logins:" + day.format(DAY));
        Map<Object, Object> c = redis.opsForHash().entries("stats:completions:" + day.format(DAY));
        for (int h = 0; h < 24; h++) {
            labels.add(String.format("%02d:00", h));
            logins.add(parse(l.get(String.valueOf(h))));
            completions.add(parse(c.get(String.valueOf(h))));
        }
    }

    private void daily(LocalDate from, LocalDate to, List<String> labels, List<Long> logins, List<Long> completions) {
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            labels.add(d.format(DateTimeFormatter.ofPattern("dd/MM")));
            logins.add(sum(redis.opsForHash().entries("stats:logins:" + d.format(DAY))));
            completions.add(sum(redis.opsForHash().entries("stats:completions:" + d.format(DAY))));
        }
    }

    private static long parse(Object v) { return v == null ? 0 : Long.parseLong(v.toString()); }

    private static long sum(Map<Object, Object> hash) {
        return hash.values().stream().mapToLong(StatsService::parse).sum();
    }

    /**
     * Таблица "Today / Week" на главной: значение + изменение в % к предыдущему такому же периоду.
     * Логины и завершения — суммы счётчиков Redis по дням, "users with activity" — distinct userId в MongoDB.
     */
    public Map<String, Object> summary() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return Map.of(
                "today", period(today, today, today.minusDays(1), today.minusDays(1)),
                "week", period(today.minusDays(6), today, today.minusDays(13), today.minusDays(7)));
    }

    private Map<String, Object> period(LocalDate from, LocalDate to, LocalDate prevFrom, LocalDate prevTo) {
        return Map.of(
                "logins", metric(redisSum("logins", from, to), redisSum("logins", prevFrom, prevTo)),
                "courseCompletions", metric(redisSum("completions", from, to), redisSum("completions", prevFrom, prevTo)),
                "usersWithActivity", metric(activeUsers(from, to), activeUsers(prevFrom, prevTo)));
    }

    private static Map<String, Long> metric(long value, long previous) {
        long change = previous == 0 ? (value > 0 ? 100 : 0) : Math.round((value - previous) * 100.0 / previous);
        return Map.of("value", value, "previous", previous, "changePercent", change);
    }

    private long redisSum(String metric, LocalDate from, LocalDate to) {
        long total = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1))
            total += sum(redis.opsForHash().entries("stats:" + metric + ":" + d.format(DAY)));
        return total;
    }

    private long activeUsers(LocalDate from, LocalDate to) {
        Query q = new Query(Criteria.where("occurredAt")
                .gte(from.atStartOfDay().toInstant(ZoneOffset.UTC))
                .lt(to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
                .and("userId").ne(null));
        return mongo.findDistinct(q, "userId", ActivityEvent.class, String.class).stream().filter(s -> !s.isBlank()).count();
    }

    /** Топ активных пользователей из Redis sorted set (ZREVRANGE WITHSCORES). */
    public List<Map<String, Object>> topUsers(int limit) {
        Set<ZSetOperations.TypedTuple<String>> top = redis.opsForZSet().reverseRangeWithScores(LEADERBOARD, 0, limit - 1);
        List<Map<String, Object>> result = new ArrayList<>();
        if (top == null) return result;
        for (var t : top) {
            result.add(Map.of("userId", Objects.requireNonNull(t.getValue()),
                    "score", t.getScore() == null ? 0 : t.getScore().longValue()));
        }
        return result;
    }
}
