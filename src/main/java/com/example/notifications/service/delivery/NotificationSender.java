package com.example.notifications.service.delivery;

import com.example.notifications.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationSender {

    private final SmsSender smsSender;
    private final EmailSender emailSender;
    private final PushSender pushSender;

    public void send(Notification notification) {
        switch (notification.getChannel()) {
            case SMS -> smsSender.send(notification);
            case EMAIL -> emailSender.send(notification);
            case PUSH -> pushSender.send(notification);
        }
    }
}
