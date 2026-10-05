/*
 * Корневой build-файл. Здесь описываем то, что общее для всех модулей:
 * версии плагинов, Java 21, репозитории, тестовый движок.
 * Каждый сервис в своём build.gradle.kts добавляет только свои зависимости.
 */
plugins {
    java
    id("org.springframework.boot") version "3.5.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.google.protobuf") version "0.9.4" apply false
}

allprojects {
    group = "kz.jazz.lms"
    version = "0.1.0"
    repositories { mavenCentral() }
}

// Единая версия gRPC для всех модулей: grpc-spring-boot-starter тянет старую grpc-core,
// а сгенерированный код — новую grpc-api; без выравнивания будет NoClassDefFoundError.
val grpcVersion by extra("1.76.0")

subprojects {
    apply(plugin = "java")

    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "io.grpc") useVersion(grpcVersion)
        }
    }

    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging { events("passed", "skipped", "failed") }
    }
}
