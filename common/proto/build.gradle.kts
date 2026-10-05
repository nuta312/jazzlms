/*
 * Модуль с gRPC-контрактами.
 * Из .proto файлов плагин protobuf генерирует Java-классы (сообщения + stub'ы).
 * user-service реализует серверную часть, course-service использует клиентский stub.
 */
import com.google.protobuf.gradle.id

plugins {
    `java-library`
    id("com.google.protobuf")
}

val grpcVersion: String by rootProject.extra   // задаётся в корневом build.gradle.kts
val protobufVersion = "3.25.8"

dependencies {
    api("io.grpc:grpc-protobuf:$grpcVersion")
    api("io.grpc:grpc-stub:$grpcVersion")
    api("com.google.protobuf:protobuf-java:$protobufVersion")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53") // @Generated для сгенерированного кода
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:$protobufVersion" }
    plugins {
        id("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion" }
    }
    generateProtoTasks {
        all().forEach { it.plugins { id("grpc") } }
    }
}
