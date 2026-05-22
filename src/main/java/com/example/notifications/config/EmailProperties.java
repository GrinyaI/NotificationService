package com.example.notifications.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification.email")
@Data
public class EmailProperties {

    private Provider provider = Provider.SIMULATED;
    private YandexSmtp yandexSmtp = new YandexSmtp();

    public enum Provider {
        SIMULATED,
        YANDEX_SMTP
    }

    @Data
    public static class YandexSmtp {
        private String host = "smtp.yandex.ru";
        private int port = 465;
        private String username;
        private String password;
        private String from;
        private String subject = "NotificationService notification";
        private boolean sslEnabled = true;
        private boolean starttlsEnabled;
        private int connectionTimeoutMs = 10000;
        private int timeoutMs = 10000;
        private int writeTimeoutMs = 10000;
    }
}
