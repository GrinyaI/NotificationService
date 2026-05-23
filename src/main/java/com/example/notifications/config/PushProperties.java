package com.example.notifications.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification.push")
@Data
public class PushProperties {

    private Provider provider = Provider.SIMULATED;
    private Fcm fcm = new Fcm();

    public enum Provider {
        SIMULATED,
        FCM
    }

    @Data
    public static class Fcm {
        private String projectId;
        private String credentialsPath;
        private String credentialsBase64;
        private String defaultTitle = "NotificationService";
    }
}
