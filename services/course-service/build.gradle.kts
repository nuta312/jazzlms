/*
 * course-service: курсы, категории, записи на курс (enrollments).
 * Технологии: PostgreSQL + JPA + Flyway, Kafka (producer), gRPC (client -> user-service),
 * MinIO / S3 (файлы уроков).
 */
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":common:proto"))
    implementation(project(":common:events"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.springframework.kafka:spring-kafka")

    // Объектное хранилище для видео и презентаций (S3 API)
    implementation("io.minio:minio:8.5.17")

    // gRPC client: @GrpcClient("user-service")
    implementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

    // Метрики в формате Prometheus: /actuator/prometheus
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
