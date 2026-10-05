package kz.jazz.lms.analytics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("JazzLMS · analytics-service")
                .description("Дашборд, таймлайн, топ пользователей").version("0.1.0"));
    }
}
