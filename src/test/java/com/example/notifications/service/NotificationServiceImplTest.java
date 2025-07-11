package com.example.notifications.service;

import com.example.notifications.dto.NotificationRequest;
import com.example.notifications.dto.NotificationResponse;
import com.example.notifications.entity.Notification;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import com.example.notifications.mapper.NotificationMapper;
import com.example.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private final String recipient = "user@example.com";
    private final String payload = "Hello!";
    @Mock
    private NotificationRepository repository;
    @Mock
    private NotificationMapper mapper;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @InjectMocks
    private NotificationServiceImpl service;
    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @Test
    void createNotifications_ShouldSaveAndSendForEachChannel() {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .recipientId(recipient)
                .payload(payload)
                .channels(List.of(Channel.EMAIL, Channel.SMS))
                .build();

        when(repository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        when(mapper.toDto(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            return NotificationResponse.builder()
                    .id(n.getId())
                    .recipientId(n.getRecipientId())
                    .channel(n.getChannel())
                    .payload(n.getPayload())
                    .deliveryStatus(n.getStatus())
                    .createdAt(n.getCreatedAt())
                    .build();
        });

        // When
        List<NotificationResponse> responses = service.createNotifications(request);

        // Then
        verify(repository, times(2)).save(notificationCaptor.capture());
        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), anyString());

        List<Notification> saved = notificationCaptor.getAllValues();
        assert saved.stream().allMatch(n -> n.getRecipientId().equals(recipient));
        assert responses.size() == 2;
    }

    @Test
    void getAll_ShouldReturnPageOfResponses() {
        // Given
        UUID id = UUID.randomUUID();
        Notification entity = Notification.builder()
                .id(id)
                .recipientId(recipient)
                .channel(Channel.PUSH)
                .payload(payload)
                .status(Status.PENDING)
                .createdAt(Instant.now())
                .build();
        Page<Notification> pageEnt = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);
        when(repository.findByRecipientIdAndChannelInAndStatusIn(
                eq(recipient), anyList(), anyList(), any(PageRequest.class)))
                .thenReturn(pageEnt);
        when(mapper.toDto(entity)).thenReturn(
                NotificationResponse.builder()
                        .id(id)
                        .recipientId(recipient)
                        .channel(Channel.PUSH)
                        .payload(payload)
                        .deliveryStatus(Status.PENDING)
                        .createdAt(entity.getCreatedAt())
                        .build());

        // When
        Page<NotificationResponse> result = service.getAll(recipient, null, null, 0, 10);

        // Then
        verify(repository).findByRecipientIdAndChannelInAndStatusIn(
                eq(recipient), anyList(), anyList(), any(PageRequest.class));
        assert result.getTotalElements() == 1;
        assert result.getContent().get(0).getId().equals(id);
    }

    @Test
    void markAsRead_ShouldSetFlagAndSave() {
        // Given
        UUID id = UUID.randomUUID();
        Notification entity = Notification.builder().id(id).isRead(false).build();
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        // When
        service.markAsRead(id);

        // Then
        assert entity.getIsRead();
        verify(repository).save(entity);
    }

    @Test
    void markAsUnread_ShouldClearFlagAndSave() {
        // Given
        UUID id = UUID.randomUUID();
        Notification entity = Notification.builder().id(id).isRead(true).build();
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        // When
        service.markAsUnread(id);

        // Then
        assert !entity.getIsRead();
        verify(repository).save(entity);
    }
}