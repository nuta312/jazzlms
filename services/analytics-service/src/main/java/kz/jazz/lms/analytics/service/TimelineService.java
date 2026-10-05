package kz.jazz.lms.analytics.service;

import kz.jazz.lms.analytics.domain.ActivityEvent;
import kz.jazz.lms.events.EventType;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Отчёт Timeline (как Reports -> Timeline в TalentLMS): фильтры + пагинация + CSV.
 *
 * Фильтры необязательные и комбинируются, поэтому запрос собираем динамически через MongoTemplate
 * и Criteria (аналог WHERE ... AND ... в SQL). Имена методов репозитория тут не подошли бы:
 * пришлось бы писать метод на каждую комбинацию фильтров.
 */
@Service
public class TimelineService {

    public record Filter(LocalDate from, LocalDate to, EventType type, String userId, String courseId) {}

    public record Page(List<ActivityEvent> items, long total, int page, int size) {}

    private static final int EXPORT_LIMIT = 5000;

    private final MongoTemplate mongo;

    public TimelineService(MongoTemplate mongo) { this.mongo = mongo; }

    public Page find(Filter f, int page, int size) {
        size = Math.max(1, Math.min(size, 100));
        page = Math.max(0, page);
        Query q = query(f);
        long total = mongo.count(q, ActivityEvent.class);              // сколько всего под фильтр
        q.with(Sort.by(Sort.Direction.DESC, "occurredAt")).skip((long) page * size).limit(size);
        return new Page(mongo.find(q, ActivityEvent.class), total, page, size);
    }

    /**
     * Активность одного пользователя для отчёта Users -> Progress: серии логинов и завершений по часам (today/yesterday)
     * или по дням (week/month/year). Redis-счётчики общие на портал, поэтому здесь считаем из MongoDB.
     */
    public Map<String, Object> userActivity(String userId, String period) {
        return activity(Criteria.where("userId").is(userId), List.of(EventType.USER_LOGGED_IN, EventType.COURSE_COMPLETED), period, userId);
    }

    /** Course -> Reports: назначения на курс и завершения по периодам. */
    public Map<String, Object> courseActivity(String courseId, String period) {
        return activity(Criteria.where("payload.courseId").is(courseId), List.of(EventType.USER_ENROLLED, EventType.COURSE_COMPLETED), period, null);
    }

    private Map<String, Object> activity(Criteria scope, List<EventType> types, String period, String userIdForLoginStats) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        boolean hourly = period.equals("today") || period.equals("yesterday");
        LocalDate from = switch (period) { case "yesterday" -> today.minusDays(1); case "week" -> today.minusDays(6);
            case "month" -> today.minusDays(29); case "year" -> today.minusDays(364); default -> today; };
        LocalDate to = period.equals("yesterday") ? today.minusDays(1) : today;

        Query q = new Query(new Criteria().andOperator(
                scope,
                Criteria.where("type").in(types),
                Criteria.where("occurredAt").gte(from.atStartOfDay().toInstant(ZoneOffset.UTC)).lt(to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))));
        List<ActivityEvent> events = mongo.find(q, ActivityEvent.class);

        List<String> labels = new ArrayList<>();
        Map<String, long[]> buckets = new LinkedHashMap<>();   // label -> [logins, completions]
        if (hourly) for (int h = 0; h < 24; h++) buckets.put(String.format("%02d:00", h), new long[2]);
        else for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) buckets.put(d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")), new long[2]);
        for (ActivityEvent e : events) {
            var t = java.time.LocalDateTime.ofInstant(e.getOccurredAt(), ZoneOffset.UTC);
            String key = hourly ? String.format("%02d:00", t.getHour()) : t.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
            long[] b = buckets.get(key);
            if (b != null) b[e.getType() == types.get(0) ? 0 : 1]++;
        }
        List<Long> logins = new ArrayList<>(), completions = new ArrayList<>();
        buckets.forEach((k, v) -> { labels.add(k); logins.add(v[0]); completions.add(v[1]); });

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("period", period); m.put("labels", labels); m.put("logins", logins); m.put("completions", completions);
        if (userIdForLoginStats != null) {
            m.put("loginsLastWeek", count(userIdForLoginStats, EventType.USER_LOGGED_IN, today.minusDays(6)));
            m.put("loginsLastMonth", count(userIdForLoginStats, EventType.USER_LOGGED_IN, today.minusDays(29)));
        }
        return m;
    }

    private long count(String userId, EventType type, LocalDate from) {
        return mongo.count(new Query(new Criteria().andOperator(Criteria.where("userId").is(userId), Criteria.where("type").is(type),
                Criteria.where("occurredAt").gte(from.atStartOfDay().toInstant(ZoneOffset.UTC)))), ActivityEvent.class);
    }

    /** CSV: дата, тип, пользователь, курс, описание. Кавычки экранируются удвоением (RFC 4180). */
    public String exportCsv(Filter f) {
        Query q = query(f).with(Sort.by(Sort.Direction.DESC, "occurredAt")).limit(EXPORT_LIMIT);
        StringBuilder sb = new StringBuilder("date,event,user,course,description\n");
        for (ActivityEvent e : mongo.find(q, ActivityEvent.class)) {
            Map<String, String> p = e.getPayload() == null ? Map.of() : e.getPayload();
            String user = (p.getOrDefault("firstName", "") + " " + p.getOrDefault("lastName", "")).trim();
            sb.append(csv(String.valueOf(e.getOccurredAt()))).append(',')
              .append(csv(e.getType().name())).append(',')
              .append(csv(user.isEmpty() ? Objects.toString(e.getUserId(), "") : user)).append(',')
              .append(csv(p.getOrDefault("courseName", ""))).append(',')
              .append(csv(e.getDescription())).append('\n');
        }
        return sb.toString();
    }

    /** Сколько событий каждого типа — агрегация на стороне MongoDB ($group), а не перебор в Java. */
    public Map<String, Long> countByType() {
        var agg = Aggregation.newAggregation(Aggregation.group("type").count().as("count"));
        Map<String, Long> result = new TreeMap<>();
        for (Document d : mongo.aggregate(agg, "activity_events", Document.class).getMappedResults()) {
            result.put(String.valueOf(d.get("_id")), ((Number) d.get("count")).longValue());
        }
        return result;
    }

    private static Query query(Filter f) {
        List<Criteria> and = new ArrayList<>();
        if (f.from() != null || f.to() != null) {
            Criteria date = Criteria.where("occurredAt");
            if (f.from() != null) date = date.gte(f.from().atStartOfDay().toInstant(ZoneOffset.UTC));
            if (f.to() != null) date = date.lt(f.to().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
            and.add(date);
        }
        if (f.type() != null) and.add(Criteria.where("type").is(f.type()));
        if (f.userId() != null && !f.userId().isBlank()) and.add(Criteria.where("userId").is(f.userId()));
        // courseId лежит внутри вложенного документа payload — Mongo умеет фильтровать по "пути"
        if (f.courseId() != null && !f.courseId().isBlank()) and.add(Criteria.where("payload.courseId").is(f.courseId()));
        return and.isEmpty() ? new Query() : new Query(new Criteria().andOperator(and));
    }

    private static String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
