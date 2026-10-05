package kz.jazz.lms.notification.config;

import kz.jazz.lms.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Consumer тоже объявляет топики, которые читает.
 * Иначе при первом запуске он подпишется раньше, чем producer создаст топик,
 * и узнает о нём только после обновления метаданных (metadata.max.age.ms = 5 минут).
 * Создание идемпотентно: если топик уже есть, ничего не происходит.
 */
@Configuration
public class KafkaTopicConfig {
    @Bean NewTopic userEvents() { return TopicBuilder.name(Topics.USER_EVENTS).partitions(3).replicas(1).build(); }
    @Bean NewTopic courseEvents() { return TopicBuilder.name(Topics.COURSE_EVENTS).partitions(3).replicas(1).build(); }
    @Bean NewTopic enrollmentEvents() { return TopicBuilder.name(Topics.ENROLLMENT_EVENTS).partitions(3).replicas(1).build(); }
}
