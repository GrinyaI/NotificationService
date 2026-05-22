package com.example.notifications.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class YandexSmtpMailConfig {

    private final EmailProperties emailProperties;

    @Bean
    public JavaMailSender yandexJavaMailSender() {
        EmailProperties.YandexSmtp yandexSmtp = emailProperties.getYandexSmtp();
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(yandexSmtp.getHost());
        sender.setPort(yandexSmtp.getPort());
        sender.setUsername(yandexSmtp.getUsername());
        sender.setPassword(yandexSmtp.getPassword());

        Properties mailProperties = sender.getJavaMailProperties();
        mailProperties.put("mail.smtp.auth", "true");
        mailProperties.put("mail.smtp.ssl.enable", Boolean.toString(yandexSmtp.isSslEnabled()));
        mailProperties.put("mail.smtp.starttls.enable", Boolean.toString(yandexSmtp.isStarttlsEnabled()));
        mailProperties.put("mail.smtp.connectiontimeout", Integer.toString(yandexSmtp.getConnectionTimeoutMs()));
        mailProperties.put("mail.smtp.timeout", Integer.toString(yandexSmtp.getTimeoutMs()));
        mailProperties.put("mail.smtp.writetimeout", Integer.toString(yandexSmtp.getWriteTimeoutMs()));
        return sender;
    }
}
