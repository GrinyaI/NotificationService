package com.example.notifications.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic emailTopic() {
        return TopicBuilder.name("notifications.email")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic smsTopic() {
        return TopicBuilder.name("notifications.sms")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic pushTopic() {
        return TopicBuilder.name("notifications.push")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
