package com.example.notifications.service.delivery;

import com.example.notifications.config.ExolveProperties;
import com.example.notifications.config.SmsProperties;
import com.example.notifications.entity.Notification;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsSenderTest {

    @Mock
    private ExolveClient exolveClient;

    private SmsProperties smsProperties;
    private ExolveProperties exolveProperties;
    private SmsSender sender;

    @BeforeEach
    void setUp() {
        smsProperties = new SmsProperties();
        exolveProperties = new ExolveProperties();
        sender = new SmsSender(smsProperties, exolveProperties, exolveClient);
    }

    @Test
    void send_ShouldSimulateByDefault() {
        Notification notification = notification("79048269449");

        sender.send(notification);

        verify(exolveClient, never()).send("79991112233", notification.getDestination(), notification.getPayload());
    }

    @Test
    void send_ShouldCallExolveWhenProviderIsExolve() {
        smsProperties.setProvider(SmsProperties.Provider.EXOLVE);
        exolveProperties.setApiKey("api-key");
        exolveProperties.setSenderNumber("79991112233");
        Notification notification = notification("79048269449");
        when(exolveClient.send("79991112233", notification.getDestination(), notification.getPayload()))
                .thenReturn(successResponse());

        sender.send(notification);

        verify(exolveClient).send("79991112233", notification.getDestination(), notification.getPayload());
    }

    @Test
    void send_ShouldRejectMissingExolveApiKey() {
        smsProperties.setProvider(SmsProperties.Provider.EXOLVE);
        exolveProperties.setSenderNumber("79991112233");
        Notification notification = notification("79048269449");

        assertThatThrownBy(() -> sender.send(notification))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key");
        verify(exolveClient, never()).send("79991112233", notification.getDestination(), notification.getPayload());
    }

    @Test
    void send_ShouldRejectMissingExolveSenderNumber() {
        smsProperties.setProvider(SmsProperties.Provider.EXOLVE);
        exolveProperties.setApiKey("api-key");
        Notification notification = notification("79048269449");

        assertThatThrownBy(() -> sender.send(notification))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sender number");
        verify(exolveClient, never()).send("", notification.getDestination(), notification.getPayload());
    }

    private Notification notification(String destination) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .channel(Channel.SMS)
                .destination(destination)
                .payload("hello")
                .status(Status.PENDING)
                .build();
    }

    private ExolveSendSmsResponse successResponse() {
        ExolveSendSmsResponse response = new ExolveSendSmsResponse();
        response.setMessageId("439166538239448536");
        response.setTemplateResourceId(136519L);
        return response;
    }
}
