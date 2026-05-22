package com.example.notifications.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification.sms")
@Data
public class SmsProperties {

    private Provider provider = Provider.SIMULATED;

    public enum Provider {
        SIMULATED,
        EXOLVE
    }
}
