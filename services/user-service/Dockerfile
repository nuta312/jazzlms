# Образ собирается из уже готового jar (./gradlew bootJar).
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8081 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
