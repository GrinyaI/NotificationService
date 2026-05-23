package com.example.notifications.service;

import com.example.notifications.entity.enums.Status;
import com.example.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationProcessingFailureService {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessingFailureService.class);

    private final NotificationRepository repository;

    @Transactional
    @CacheEvict(cacheNames = "notification", key = "#key")
    public void markFailed(String key, Exception exception) {
        UUID notificationId = parseNotificationId(key);
        if (notificationId == null) {
            return;
        }

        repository.findById(notificationId).ifPresentOrElse(notification -> {
            notification.setStatus(Status.FAILED);
            notification.setErrorDescription(errorMessage(exception));
            repository.save(notification);
        }, () -> log.warn("Unable to mark missing notification {} as failed", notificationId));
    }

    private UUID parseNotificationId(String key) {
        try {
            return UUID.fromString(key);
        } catch (RuntimeException e) {
            log.warn("Unable to mark failed notification because Kafka key is not UUID: {}", key);
            return null;
        }
    }

    private String errorMessage(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        return cause.getMessage();
    }
}
