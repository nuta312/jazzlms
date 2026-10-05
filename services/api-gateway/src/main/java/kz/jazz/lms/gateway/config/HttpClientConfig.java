package kz.jazz.lms.gateway.config;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reactor Netty по умолчанию кэширует DNS-ответы надолго. В docker при пересоздании
 * контейнера (docker compose up --build course-service) у сервиса меняется IP,
 * и gateway продолжал бы стучаться по старому адресу ("Connection refused").
 * Переключаемся на JVM-резолвер: он кэширует всего ~30 секунд.
 */
@Configuration
public class HttpClientConfig {
    @Bean
    public HttpClientCustomizer jvmDnsResolver() {
        return httpClient -> httpClient.resolver(DefaultAddressResolverGroup.INSTANCE);
    }
}
