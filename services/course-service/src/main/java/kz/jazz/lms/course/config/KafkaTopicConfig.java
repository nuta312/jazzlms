package kz.jazz.lms.course.config;

import kz.jazz.lms.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic courseEvents() {
        return TopicBuilder.name(Topics.COURSE_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic enrollmentEvents() {
        return TopicBuilder.name(Topics.ENROLLMENT_EVENTS).partitions(3).replicas(1).build();
    }
}
