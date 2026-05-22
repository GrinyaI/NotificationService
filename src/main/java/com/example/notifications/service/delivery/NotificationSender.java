package com.example.notifications.service.delivery;

import com.example.notifications.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);

    private final SmsSender smsSender;
    private final EmailSender emailSender;

    public void send(Notification notification) {
        switch (notification.getChannel()) {
            case SMS -> smsSender.send(notification);
            case EMAIL -> emailSender.send(notification);
            case PUSH -> log.info("Simulated {} notification delivery {}: {}",
                    notification.getChannel(),
                    notification.getId(),
                    notification.getPayload());
        }
    }
}
