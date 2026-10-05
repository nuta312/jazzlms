/*
 * gamification-service: очки, уровни, бейджи, лидерборд (как Gamification в TalentLMS).
 * Технологии: Kafka (consumer), MongoDB (профили), Redis ZSET (лидерборды), gRPC (client -> user-service, имена).
 * Сервис "чисто событийный": у него нет ни одной команды от пользователя — всё, что он знает, пришло из Kafka.
 */
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":common:proto"))
    implementation(project(":common:events"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

    // Метрики в формате Prometheus: /actuator/prometheus
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
