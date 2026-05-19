package com.example.notifications.config;

import com.example.notifications.entity.enums.Channel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "kafka.topic")
@Data
public class KafkaTopicProperties {

    private String email;
    private String sms;
    private String push;
    private int partitions = 3;
    private short replicas = 1;

    public String topicFor(Channel channel) {
        return switch (channel) {
            case EMAIL -> email;
            case SMS -> sms;
            case PUSH -> push;
        };
    }

    public List<String> notificationTopics() {
        return List.of(email, sms, push);
    }
}
