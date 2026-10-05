/*
 * user-service: пользователи, аутентификация (JWT), роли.
 * Технологии: PostgreSQL + JPA + Flyway, Redis (кэш), Kafka (producer), gRPC (server).
 */
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":common:proto"))
    implementation(project(":common:events"))

    // REST API
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // SQL: PostgreSQL + JPA (ORM) + Flyway (миграции схемы)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Redis как кэш (@Cacheable)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Kafka producer
    implementation("org.springframework.kafka:spring-kafka")

    // gRPC server
    implementation("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")

    // Хэширование паролей (BCrypt) + JWT
    implementation("org.springframework.security:spring-security-crypto")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Swagger UI: /swagger-ui.html
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

    // Распределённая трассировка -> Zipkin
    // Метрики в формате Prometheus: /actuator/prometheus
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}
