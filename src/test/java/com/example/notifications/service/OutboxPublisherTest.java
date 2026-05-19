package com.example.notifications.service;

import com.example.notifications.entity.OutboxMessage;
import com.example.notifications.entity.enums.OutboxStatus;
import com.example.notifications.repository.OutboxMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxMessageRepository outboxRepository;
    @Mock
    private NotificationProcessingFailureService failureService;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private TransactionTemplate transactionTemplate;
    @InjectMocks
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "maxAttempts", 3);
        ReflectionTestUtils.setField(publisher, "sendTimeoutSeconds", 1L);
        ReflectionTestUtils.setField(publisher, "batchSize", 50);
        ReflectionTestUtils.setField(publisher, "processingTimeoutSeconds", 60L);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        doAnswer(invocation -> {
            Consumer<Object> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void publishPending_ShouldClaimAndPublishMessageOnce() {
        OutboxMessage message = message(OutboxStatus.PENDING, 0);

        when(outboxRepository.findPendingForUpdate(50)).thenReturn(List.of(message));
        when(outboxRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send("notifications.email", "message-key", "payload"))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPending();

        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(message.getAttempts()).isZero();
        assertThat(message.getPublishedAt()).isNotNull();
        verify(outboxRepository).resetStaleProcessing(any(Instant.class));
        verify(failureService, never()).markFailed(any(), any());
    }

    @Test
    void publishPending_ShouldReturnMessageToPendingWhenPublishFailsBeforeMaxAttempts() {
        OutboxMessage message = message(OutboxStatus.PENDING, 0);

        when(outboxRepository.findPendingForUpdate(50)).thenReturn(List.of(message));
        when(outboxRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send("notifications.email", "message-key", "payload"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable")));

        publisher.publishPending();

        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(message.getAttempts()).isEqualTo(1);
        assertThat(message.getLastError()).contains("Kafka unavailable");
        verify(failureService, never()).markFailed(any(), any());
    }

    @Test
    void publishPending_ShouldMarkFailedWhenPublishAttemptsAreExhausted() {
        OutboxMessage message = message(OutboxStatus.PENDING, 2);

        when(outboxRepository.findPendingForUpdate(50)).thenReturn(List.of(message));
        when(outboxRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send("notifications.email", "message-key", "payload"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable")));

        publisher.publishPending();

        assertThat(message.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(message.getAttempts()).isEqualTo(3);
        verify(failureService).markFailed(any(), any());
    }

    private OutboxMessage message(OutboxStatus status, int attempts) {
        return OutboxMessage.builder()
                .id(UUID.randomUUID())
                .notificationId(UUID.randomUUID())
                .topic("notifications.email")
                .messageKey("message-key")
                .payload("payload")
                .status(status)
                .attempts(attempts)
                .createdAt(Instant.now())
                .build();
    }
}
