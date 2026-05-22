package com.example.notifications.service.delivery;

import com.example.notifications.config.ExolveProperties;
import com.example.notifications.config.SmsProperties;
import com.example.notifications.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SmsSender {

    private static final Logger log = LoggerFactory.getLogger(SmsSender.class);

    private final SmsProperties smsProperties;
    private final ExolveProperties exolveProperties;
    private final ExolveClient exolveClient;

    public void send(Notification notification) {
        validateDestination(notification);

        switch (smsProperties.getProvider()) {
            case SIMULATED -> simulate(notification);
            case EXOLVE -> sendViaExolve(notification);
        }
    }

    private void simulate(Notification notification) {
        log.info("Simulated SMS notification delivery {}: {}", notification.getId(), notification.getPayload());
    }

    private void sendViaExolve(Notification notification) {
        validateExolveConfiguration();
        ExolveSendSmsResponse response = exolveClient.send(
                exolveProperties.getSenderNumber(),
                notification.getDestination(),
                notification.getPayload());
        log.info("MTS Exolve accepted notification {} for {} with message_id {}",
                notification.getId(),
                notification.getDestination(),
                response.getMessageId());
    }

    private void validateExolveConfiguration() {
        if (!StringUtils.hasText(exolveProperties.getApiKey())) {
            throw new IllegalArgumentException("MTS Exolve API key is required when SMS_PROVIDER=EXOLVE");
        }
        if (!StringUtils.hasText(exolveProperties.getSenderNumber())) {
            throw new IllegalArgumentException("MTS Exolve sender number is required when SMS_PROVIDER=EXOLVE");
        }
    }

    private void validateDestination(Notification notification) {
        if (!StringUtils.hasText(notification.getDestination())) {
            throw new IllegalArgumentException("SMS destination is required for notification " + notification.getId());
        }
    }
}
