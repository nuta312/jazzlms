package kz.jazz.lms.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * По какому ключу считать лимит: если пользователь известен (JWT прошёл) — по userId,
 * иначе — по IP. Счётчики лежат в Redis, поэтому лимит общий для всех экземпляров gateway.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null) return Mono.just("user:" + userId);
            String ip = Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress();
            return Mono.just("ip:" + ip);
        };
    }
}
