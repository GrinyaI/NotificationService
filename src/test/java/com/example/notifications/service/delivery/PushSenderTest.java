package com.example.notifications.service.delivery;

import com.example.notifications.config.PushProperties;
import com.example.notifications.entity.Notification;
import com.example.notifications.entity.PushSubscription;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import com.example.notifications.repository.PushSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushSenderTest {

    @Mock
    private PushSubscriptionRepository subscriptionRepository;
    @Mock
    private ObjectProvider<FcmClient> fcmClientProvider;
    @Mock
    private FcmClient fcmClient;

    private PushProperties pushProperties;
    private PushSender sender;

    @BeforeEach
    void setUp() {
        pushProperties = new PushProperties();
        sender = new PushSender(pushProperties, subscriptionRepository, fcmClientProvider);
    }

    @Test
    void send_ShouldSimulateByDefault() {
        Notification notification = notification();

        sender.send(notification);

        verify(subscriptionRepository, never()).findByRecipientIdAndActiveTrue("user-1");
        verify(fcmClient, never()).send(eq("token"), eq("title"), eq("body"), argThat(anyMap()));
    }

    @Test
    void send_ShouldSendToActiveFcmSubscriptions() {
        pushProperties.setProvider(PushProperties.Provider.FCM);
        pushProperties.getFcm().setDefaultTitle("NotificationService");
        Notification notification = notification();
        PushSubscription subscription = subscription("token");
        when(subscriptionRepository.findByRecipientIdAndActiveTrue("user-1"))
                .thenReturn(List.of(subscription));
        when(fcmClientProvider.getIfAvailable()).thenReturn(fcmClient);
        when(fcmClient.send(
                eq("token"),
                eq("NotificationService"),
                eq("hello"),
                argThat(dataFor(notification))
        )).thenReturn("projects/demo/messages/1");

        sender.send(notification);

        verify(fcmClient).send(
                eq("token"),
                eq("NotificationService"),
                eq("hello"),
                argThat(dataFor(notification))
        );
    }

    @Test
    void send_ShouldRejectRecipientWithoutSubscriptions() {
        pushProperties.setProvider(PushProperties.Provider.FCM);
        Notification notification = notification();
        when(subscriptionRepository.findByRecipientIdAndActiveTrue("user-1")).thenReturn(List.of());

        assertThatThrownBy(() -> sender.send(notification))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No active FCM push subscriptions");
    }

    @Test
    void send_ShouldDeactivateInvalidToken() {
        pushProperties.setProvider(PushProperties.Provider.FCM);
        Notification notification = notification();
        PushSubscription subscription = subscription("invalid-token");
        when(subscriptionRepository.findByRecipientIdAndActiveTrue("user-1"))
                .thenReturn(List.of(subscription));
        when(fcmClientProvider.getIfAvailable()).thenReturn(fcmClient);
        when(fcmClient.send(
                eq("invalid-token"),
                eq("NotificationService"),
                eq("hello"),
                argThat(dataFor(notification))
        )).thenThrow(new PushDeliveryException("invalid token", null, true));

        assertThatThrownBy(() -> sender.send(notification))
                .isInstanceOf(PushDeliveryException.class)
                .hasMessageContaining("failed for all subscriptions");
        assertThat(subscription.getActive()).isFalse();
        verify(subscriptionRepository).save(subscription);
    }

    private Notification notification() {
        return Notification.builder()
                .id(UUID.randomUUID())
                .recipientId("user-1")
                .channel(Channel.PUSH)
                .payload("hello")
                .status(Status.PENDING)
                .build();
    }

    private PushSubscription subscription(String token) {
        return PushSubscription.builder()
                .id(UUID.randomUUID())
                .recipientId("user-1")
                .fcmToken(token)
                .platform("WEB")
                .active(true)
                .build();
    }

    private ArgumentMatcher<Map<String, String>> dataFor(Notification notification) {
        return data -> data != null
                && notification.getId().toString().equals(data.get("notificationId"))
                && "PUSH".equals(data.get("channel"))
                && "user-1".equals(data.get("recipientId"));
    }

    private ArgumentMatcher<Map<String, String>> anyMap() {
        return data -> true;
    }
}
