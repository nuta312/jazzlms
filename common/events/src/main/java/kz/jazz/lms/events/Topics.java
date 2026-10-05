package kz.jazz.lms.events;

/**
 * Имена Kafka-топиков. Вынесены в константы, чтобы producer и consumer
 * не разъехались из-за опечатки в строке.
 */
public final class Topics {
    public static final String USER_EVENTS = "user-events";
    public static final String COURSE_EVENTS = "course-events";
    public static final String ENROLLMENT_EVENTS = "enrollment-events";

    private Topics() {}
}
