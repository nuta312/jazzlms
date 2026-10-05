package kz.jazz.lms.gamification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("JazzLMS · gamification-service")
                .description("Очки, уровни, бейджи, лидерборд").version("0.1.0"));
    }
}
