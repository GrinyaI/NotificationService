package com.example.notifications.service;

import com.example.notifications.entity.OutboxMessage;
import com.example.notifications.entity.enums.OutboxStatus;
import com.example.notifications.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxMessageRepository outboxRepository;
    private final NotificationProcessingFailureService failureService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${notification.outbox.max-attempts}")
    private int maxAttempts;
    @Value("${notification.outbox.send-timeout-seconds}")
    private long sendTimeoutSeconds;
    @Value("${notification.outbox.batch-size}")
    private int batchSize;
    @Value("${notification.outbox.processing-timeout-seconds}")
    private long processingTimeoutSeconds;

    @Scheduled(
            fixedDelayString = "${notification.outbox.publish-delay-ms}",
            initialDelayString = "${notification.outbox.initial-delay-ms}"
    )
    public void publishPending() {
        resetStaleProcessing();
        claimPendingMessages().forEach(this::publish);
    }

    private void resetStaleProcessing() {
        Instant threshold = Instant.now().minusSeconds(processingTimeoutSeconds);
        transactionTemplate.executeWithoutResult(status -> outboxRepository.resetStaleProcessing(threshold));
    }

    private List<OutboxMessage> claimPendingMessages() {
        List<OutboxMessage> messages = transactionTemplate.execute(status -> {
            List<OutboxMessage> pending = outboxRepository.findPendingForUpdate(batchSize);
            Instant now = Instant.now();
            pending.forEach(message -> {
                message.setStatus(OutboxStatus.PROCESSING);
                message.setLastAttemptAt(now);
                message.setLastError(null);
            });
            return outboxRepository.saveAll(pending);
        });
        return messages == null ? List.of() : messages;
    }

    private void publish(OutboxMessage message) {
        try {
            kafkaTemplate.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);
            markPublished(message);
        } catch (Exception e) {
            markPublishFailed(message, e);
        }
    }

    private void markPublished(OutboxMessage message) {
        message.setStatus(OutboxStatus.PUBLISHED);
        message.setPublishedAt(Instant.now());
        message.setLastAttemptAt(Instant.now());
        message.setLastError(null);
        outboxRepository.save(message);
    }

    private void markPublishFailed(OutboxMessage message, Exception exception) {
        int attempts = message.getAttempts() + 1;
        message.setAttempts(attempts);
        message.setLastAttemptAt(Instant.now());
        message.setLastError(errorMessage(exception));
        if (attempts >= maxAttempts) {
            message.setStatus(OutboxStatus.FAILED);
            failureService.markFailed(message.getMessageKey(), exception);
        } else {
            message.setStatus(OutboxStatus.PENDING);
        }
        outboxRepository.save(message);
    }

    private String errorMessage(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        return cause.getMessage();
    }
}
