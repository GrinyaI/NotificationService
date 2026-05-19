package com.example.notifications.service;

import com.example.notifications.entity.Notification;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import com.example.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryProcessorTest {

    @Mock
    private NotificationRepository repository;
    @InjectMocks
    private NotificationDeliveryProcessor processor;

    @Test
    void process_ShouldMarkPendingNotificationAsSent() {
        UUID id = UUID.randomUUID();
        Notification notification = notification(id, Status.PENDING);
        when(repository.findById(id)).thenReturn(Optional.of(notification));

        processor.process(Channel.EMAIL, id.toString(), "payload");

        assertThat(notification.getStatus()).isEqualTo(Status.SENT);
        assertThat(notification.getSentAt()).isNotNull();
        verify(repository).save(notification);
    }

    @Test
    void process_ShouldSkipAlreadySentDuplicateMessage() {
        UUID id = UUID.randomUUID();
        Notification notification = notification(id, Status.SENT);
        notification.setSentAt(Instant.now());
        when(repository.findById(id)).thenReturn(Optional.of(notification));

        processor.process(Channel.EMAIL, id.toString(), "payload");

        verify(repository, never()).save(notification);
    }

    @Test
    void process_ShouldRejectMessageFromWrongChannel() {
        UUID id = UUID.randomUUID();
        Notification notification = notification(id, Status.PENDING);
        when(repository.findById(id)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> processor.process(Channel.SMS, id.toString(), "payload"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(id.toString());
        verify(repository, never()).save(notification);
    }

    private Notification notification(UUID id, Status status) {
        return Notification.builder()
                .id(id)
                .channel(Channel.EMAIL)
                .payload("payload")
                .status(status)
                .build();
    }
}
