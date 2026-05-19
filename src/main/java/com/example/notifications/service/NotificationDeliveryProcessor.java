package com.example.notifications.service;

import com.example.notifications.entity.Notification;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import com.example.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryProcessor.class);

    private final NotificationRepository repository;

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "notification", key = "#key"),
            @CacheEvict(cacheNames = "notifications", allEntries = true)
    })
    public void process(Channel channel, String key, String payload) {
        UUID notificationId = parseNotificationId(key);
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        validateMessage(channel, payload, notification);
        if (notification.getStatus() == Status.SENT) {
            log.info("Skipping duplicate {} notification delivery {}", channel, notificationId);
            return;
        }

        log.info("Simulated {} notification delivery {}: {}", channel, notificationId, payload);
        notification.setStatus(Status.SENT);
        notification.setSentAt(Instant.now());
        notification.setErrorDescription(null);
        repository.save(notification);
    }

    private void validateMessage(Channel channel, String payload, Notification notification) {
        if (notification.getChannel() != channel) {
            throw new IllegalArgumentException("Kafka topic channel does not match notification channel: "
                    + notification.getId());
        }
        if (!Objects.equals(notification.getPayload(), payload)) {
            throw new IllegalArgumentException("Kafka payload does not match notification payload: "
                    + notification.getId());
        }
    }

    private UUID parseNotificationId(String key) {
        try {
            return UUID.fromString(key);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Kafka message key must be notification UUID: " + key, e);
        }
    }
}
