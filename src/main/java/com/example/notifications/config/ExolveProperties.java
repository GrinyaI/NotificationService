package com.example.notifications.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification.sms.exolve")
@Data
public class ExolveProperties {

    private String apiKey;
    private String baseUrl = "https://api.exolve.ru";
    private String senderNumber;
}
