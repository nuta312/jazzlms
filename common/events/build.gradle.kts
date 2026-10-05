/*
 * DTO событий, которые летают через Kafka.
 * Producer (user-service, course-service) сериализует их в JSON,
 * consumer (notification-service, analytics-service) десериализует обратно.
 * Общий модуль гарантирует, что обе стороны одинаково понимают формат.
 */
plugins { `java-library` }

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
}
