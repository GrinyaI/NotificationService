package com.example.notifications.service.delivery;

import com.example.notifications.config.EmailProperties;
import com.example.notifications.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final EmailProperties emailProperties;
    private final JavaMailSender mailSender;

    public void send(Notification notification) {
        validateDestination(notification);

        switch (emailProperties.getProvider()) {
            case SIMULATED -> simulate(notification);
            case YANDEX_SMTP -> sendViaYandexSmtp(notification);
        }
    }

    private void simulate(Notification notification) {
        log.info("Simulated EMAIL notification delivery {}: {}", notification.getId(), notification.getPayload());
    }

    private void sendViaYandexSmtp(Notification notification) {
        EmailProperties.YandexSmtp yandexSmtp = emailProperties.getYandexSmtp();
        validateYandexConfiguration(yandexSmtp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(resolveFrom(yandexSmtp));
        message.setTo(notification.getDestination());
        message.setSubject(yandexSmtp.getSubject());
        message.setText(notification.getPayload());

        try {
            mailSender.send(message);
            log.info("Yandex SMTP accepted email notification {} for {}",
                    notification.getId(),
                    notification.getDestination());
        } catch (MailException e) {
            throw new EmailDeliveryException("Yandex SMTP request failed", e);
        }
    }

    private void validateYandexConfiguration(EmailProperties.YandexSmtp yandexSmtp) {
        if (!StringUtils.hasText(yandexSmtp.getUsername())) {
            throw new IllegalArgumentException("Yandex SMTP username is required when EMAIL_PROVIDER=YANDEX_SMTP");
        }
        if (!StringUtils.hasText(yandexSmtp.getPassword())) {
            throw new IllegalArgumentException("Yandex SMTP password is required when EMAIL_PROVIDER=YANDEX_SMTP");
        }
        if (!StringUtils.hasText(resolveFrom(yandexSmtp))) {
            throw new IllegalArgumentException("Yandex SMTP from address is required when EMAIL_PROVIDER=YANDEX_SMTP");
        }
        if (!StringUtils.hasText(yandexSmtp.getSubject())) {
            throw new IllegalArgumentException("Yandex SMTP subject is required when EMAIL_PROVIDER=YANDEX_SMTP");
        }
    }

    private String resolveFrom(EmailProperties.YandexSmtp yandexSmtp) {
        if (StringUtils.hasText(yandexSmtp.getFrom())) {
            return yandexSmtp.getFrom();
        }
        return yandexSmtp.getUsername();
    }

    private void validateDestination(Notification notification) {
        if (!StringUtils.hasText(notification.getDestination())) {
            throw new IllegalArgumentException("EMAIL destination is required for notification " + notification.getId());
        }
    }
}
