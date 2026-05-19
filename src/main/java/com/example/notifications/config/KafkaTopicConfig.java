package com.example.notifications.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {

    private static final String DLT_SUFFIX = ".dlt";

    private final KafkaTopicProperties topicProperties;

    @Bean
    public KafkaAdmin.NewTopics notificationTopics() {
        NewTopic[] topics = topicProperties.notificationTopics().stream()
                .flatMap(topic -> java.util.stream.Stream.of(topic, topic + DLT_SUFFIX))
                .map(this::topic)
                .toArray(NewTopic[]::new);
        return new KafkaAdmin.NewTopics(topics);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(topicProperties.getPartitions())
                .replicas(topicProperties.getReplicas())
                .build();
    }
}
