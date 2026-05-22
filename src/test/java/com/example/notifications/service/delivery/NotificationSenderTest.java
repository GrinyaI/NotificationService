package com.example.notifications.service.delivery;

import com.example.notifications.entity.Notification;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationSenderTest {

    @Mock
    private SmsSender smsSender;
    @Mock
    private EmailSender emailSender;

    private NotificationSender sender;

    @BeforeEach
    void setUp() {
        sender = new NotificationSender(smsSender, emailSender);
    }

    @Test
    void send_ShouldRouteSmsToSmsSender() {
        Notification notification = notification(Channel.SMS);

        sender.send(notification);

        verify(smsSender).send(notification);
        verify(emailSender, never()).send(notification);
    }

    @Test
    void send_ShouldRouteEmailToEmailSender() {
        Notification notification = notification(Channel.EMAIL);

        sender.send(notification);

        verify(emailSender).send(notification);
        verify(smsSender, never()).send(notification);
    }

    @Test
    void send_ShouldSimulatePush() {
        Notification notification = notification(Channel.PUSH);

        sender.send(notification);

        verify(emailSender, never()).send(notification);
        verify(smsSender, never()).send(notification);
    }

    private Notification notification(Channel channel) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .channel(channel)
                .destination("destination")
                .payload("hello")
                .status(Status.PENDING)
                .build();
    }
}
