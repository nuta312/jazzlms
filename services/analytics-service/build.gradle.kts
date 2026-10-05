/*
 * analytics-service: статистика для дашборда и таймлайн активности.
 * Технологии: Redis (счётчики HINCRBY, sorted set для топа), MongoDB (лента событий), Kafka (consumer).
 */
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":common:events"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

    // Метрики в формате Prometheus: /actuator/prometheus
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
