rootProject.name = "jazzlms"

// Общие библиотеки (используются несколькими сервисами)
include("common:proto")     // gRPC-контракты (.proto) -> Java-классы
include("common:events")    // DTO событий Kafka

// Микросервисы
include("services:api-gateway")
include("services:user-service")
include("services:course-service")
include("services:notification-service")
include("services:analytics-service")
include("services:gamification-service")
