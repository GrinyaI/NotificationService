package com.example.notifications.consumer;

import com.example.notifications.entity.enums.Status;
import com.example.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PushConsumer {
    private static final Logger log = LoggerFactory.getLogger(PushConsumer.class);

    private final NotificationRepository repository;
    private final RetryTemplate retryTemplate;

    @KafkaListener(
            topics = "${kafka.topic.push}", groupId = "notifications-push-group"
    )
    public void listen(
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Payload String value
    ) {
        UUID notificationId = UUID.fromString(key);
        retryTemplate.execute(context -> process(notificationId, value));
    }

    private Void process(UUID id, String payload) {
        try {
            log.info("Отправка PUSH уведомления {}: {}", id, payload);
            repository.findById(id).ifPresent(n -> {
                n.setStatus(Status.SENT);
                n.setSentAt(Instant.now());
                repository.save(n);
            });
        } catch (Exception e) {
            log.error("Ошибка отправки PUSH уведомления {}", id, e);
            repository.findById(id).ifPresent(n -> {
                n.setStatus(Status.FAILED);
                n.setErrorDescription(e.getMessage());
                repository.save(n);
            });
        }
        return null;
    }
}
