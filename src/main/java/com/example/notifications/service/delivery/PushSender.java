package com.example.notifications.service.delivery;

import com.example.notifications.config.PushProperties;
import com.example.notifications.entity.Notification;
import com.example.notifications.entity.PushSubscription;
import com.example.notifications.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PushSender {

    private static final Logger log = LoggerFactory.getLogger(PushSender.class);

    private final PushProperties pushProperties;
    private final PushSubscriptionRepository subscriptionRepository;
    private final ObjectProvider<FcmClient> fcmClientProvider;

    public void send(Notification notification) {
        switch (pushProperties.getProvider()) {
            case SIMULATED -> simulate(notification);
            case FCM -> sendViaFcm(notification);
        }
    }

    private void simulate(Notification notification) {
        log.info("Simulated PUSH notification delivery {}: {}", notification.getId(), notification.getPayload());
    }

    private void sendViaFcm(Notification notification) {
        String recipientId = resolveRecipientId(notification);
        List<PushSubscription> subscriptions = subscriptionRepository.findByRecipientIdAndActiveTrue(recipientId);
        if (subscriptions.isEmpty()) {
            throw new IllegalArgumentException("No active FCM push subscriptions for recipient " + recipientId);
        }

        FcmClient fcmClient = requiredFcmClient();
        int sentCount = 0;
        List<String> errors = new ArrayList<>();
        for (PushSubscription subscription : subscriptions) {
            try {
                String messageId = fcmClient.send(
                        subscription.getFcmToken(),
                        pushProperties.getFcm().getDefaultTitle(),
                        notification.getPayload(),
                        data(notification)
                );
                sentCount++;
                log.info("Firebase Cloud Messaging accepted push notification {} for {} with message_id {}",
                        notification.getId(),
                        recipientId,
                        messageId);
            } catch (PushDeliveryException e) {
                handleFcmFailure(subscription, errors, e);
            }
        }

        if (sentCount == 0) {
            throw new PushDeliveryException("Firebase Cloud Messaging failed for all subscriptions: "
                    + String.join("; ", errors));
        }
    }

    private void handleFcmFailure(
            PushSubscription subscription,
            List<String> errors,
            PushDeliveryException exception
    ) {
        errors.add(exception.getMessage());
        if (exception.isInvalidToken()) {
            subscription.setActive(false);
            subscriptionRepository.save(subscription);
        }
        log.warn("Firebase Cloud Messaging push delivery failed for subscription {}: {}",
                subscription.getId(),
                exception.getMessage());
    }

    private FcmClient requiredFcmClient() {
        FcmClient fcmClient = fcmClientProvider.getIfAvailable();
        if (fcmClient == null) {
            throw new IllegalArgumentException("Firebase Cloud Messaging client is not configured");
        }
        return fcmClient;
    }

    private Map<String, String> data(Notification notification) {
        return Map.of(
                "notificationId", notification.getId().toString(),
                "channel", notification.getChannel().name(),
                "recipientId", resolveRecipientId(notification)
        );
    }

    private String resolveRecipientId(Notification notification) {
        if (StringUtils.hasText(notification.getRecipientId())) {
            return notification.getRecipientId();
        }
        if (StringUtils.hasText(notification.getDestination())) {
            return notification.getDestination();
        }
        throw new IllegalArgumentException("PUSH recipientId is required for notification " + notification.getId());
    }
}
