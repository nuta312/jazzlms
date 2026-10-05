package kz.jazz.lms.user.config;

import kz.jazz.lms.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Spring при старте создаст топик, если его ещё нет. */
@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(Topics.USER_EVENTS).partitions(3).replicas(1).build();
    }
}
