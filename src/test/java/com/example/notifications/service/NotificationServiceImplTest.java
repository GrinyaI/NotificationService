package com.example.notifications.service;

import com.example.notifications.config.KafkaTopicProperties;
import com.example.notifications.dto.NotificationRequest;
import com.example.notifications.dto.NotificationResponse;
import com.example.notifications.entity.Notification;
import com.example.notifications.entity.OutboxMessage;
import com.example.notifications.entity.enums.AudienceType;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.NotificationPriority;
import com.example.notifications.entity.enums.OutboxStatus;
import com.example.notifications.entity.enums.Status;
import com.example.notifications.exception.DuplicateRequestConflictException;
import com.example.notifications.exception.NotificationNotFoundException;
import com.example.notifications.mapper.NotificationMapper;
import com.example.notifications.repository.NotificationRepository;
import com.example.notifications.repository.OutboxMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
    private OutboxMessageRepository outboxRepository;
    @Mock
    private NotificationMapper mapper;
    @Mock
    private KafkaTopicProperties topicProperties;
    @Mock
    private TransactionTemplate transactionTemplate;
    @InjectMocks
    private NotificationServiceImpl service;
    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;
    @Captor
    private ArgumentCaptor<OutboxMessage> outboxCaptor;

    @BeforeEach
    void setUpTransactionTemplate() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    void createNotifications_ShouldSaveAndEnqueueForEachChannel() {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .recipientId(recipient)
                .payload(payload)
                .channels(List.of(Channel.EMAIL, Channel.SMS))
                .idempotencyKey("request-create")
                .build();

        when(repository.findByIdempotencyKey("request-create")).thenReturn(List.of());
        when(topicProperties.topicFor(Channel.EMAIL)).thenReturn("notifications.email");
        when(topicProperties.topicFor(Channel.SMS)).thenReturn("notifications.sms");
        when(repository.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        when(mapper.toDto(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            return NotificationResponse.builder()
                    .id(n.getId())
                    .recipientId(n.getRecipientId())
                    .audienceType(n.getAudienceType())
                    .audienceTarget(n.getAudienceTarget())
                    .destination(n.getDestination())
                    .channel(n.getChannel())
                    .payload(n.getPayload())
                    .priority(n.getPriority())
                    .deliveryStatus(n.getStatus())
                    .createdAt(n.getCreatedAt())
                    .build();
        });

        // When
        List<NotificationResponse> responses = service.createNotifications(request);

        // Then
        verify(repository, times(2)).saveAndFlush(notificationCaptor.capture());
        verify(outboxRepository, times(2)).save(outboxCaptor.capture());

        List<Notification> saved = notificationCaptor.getAllValues();
        assertThat(saved)
                .allMatch(n -> n.getRecipientId().equals(recipient))
                .allMatch(n -> n.getAudienceType() == AudienceType.PERSONAL)
                .allMatch(n -> n.getAudienceTarget().equals(recipient))
                .allMatch(n -> n.getDestination().equals(recipient))
                .allMatch(n -> n.getPriority() == NotificationPriority.NORMAL);
        assertThat(outboxCaptor.getAllValues())
                .extracting(OutboxMessage::getTopic)
                .containsExactly("notifications.email", "notifications.sms");
        assertThat(outboxCaptor.getAllValues())
                .allMatch(o -> o.getStatus() == OutboxStatus.PENDING)
                .allMatch(o -> o.getPayload().equals(payload));
        assertThat(responses).hasSize(2);
    }

    @Test
    void createNotifications_ShouldSupportAudienceAndChannelDestinations() {
        // Given
        Instant expiresAt = Instant.now().plusSeconds(3600);
        NotificationRequest request = NotificationRequest.builder()
                .audienceType(AudienceType.SEGMENT)
                .audienceTarget("premium-users")
                .payload(payload)
                .channels(List.of(Channel.EMAIL, Channel.EMAIL, Channel.PUSH))
                .priority(NotificationPriority.URGENT)
                .expiresAt(expiresAt)
                .idempotencyKey("segment-campaign-1")
                .channelDestinations(Map.of(Channel.EMAIL, "premium@example.com"))
                .build();

        when(repository.findByIdempotencyKey("segment-campaign-1")).thenReturn(List.of());
        when(topicProperties.topicFor(Channel.EMAIL)).thenReturn("notifications.email");
        when(topicProperties.topicFor(Channel.PUSH)).thenReturn("notifications.push");
        when(repository.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
        when(mapper.toDto(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            return NotificationResponse.builder()
                    .id(n.getId())
                    .audienceType(n.getAudienceType())
                    .audienceTarget(n.getAudienceTarget())
                    .destination(n.getDestination())
                    .channel(n.getChannel())
                    .payload(n.getPayload())
                    .priority(n.getPriority())
                    .deliveryStatus(n.getStatus())
                    .expiresAt(n.getExpiresAt())
                    .idempotencyKey(n.getIdempotencyKey())
                    .build();
        });

        // When
        List<NotificationResponse> responses = service.createNotifications(request);

        // Then
        verify(repository, times(2)).saveAndFlush(notificationCaptor.capture());
        verify(outboxRepository, times(2)).save(outboxCaptor.capture());

        List<Notification> saved = notificationCaptor.getAllValues();
        assertThat(saved).extracting(Notification::getChannel)
                .containsExactly(Channel.EMAIL, Channel.PUSH);
        assertThat(saved)
                .allMatch(n -> n.getRecipientId() == null)
                .allMatch(n -> n.getAudienceType() == AudienceType.SEGMENT)
                .allMatch(n -> n.getAudienceTarget().equals("premium-users"))
                .allMatch(n -> n.getPriority() == NotificationPriority.URGENT)
                .allMatch(n -> n.getExpiresAt().equals(expiresAt))
                .allMatch(n -> n.getIdempotencyKey().equals("segment-campaign-1"));
        assertThat(saved.get(0).getDestination()).isEqualTo("premium@example.com");
        assertThat(saved.get(1).getDestination()).isNull();
        assertThat(outboxCaptor.getAllValues())
                .extracting(OutboxMessage::getTopic)
                .containsExactly("notifications.email", "notifications.push");
        assertThat(responses).hasSize(2);
    }

    @Test
    void createNotifications_ShouldReturnExistingByIdempotencyKey() {
        // Given
        String idempotencyKey = "request-1";
        UUID id = UUID.randomUUID();
        Notification existing = Notification.builder()
                .id(id)
                .recipientId(recipient)
                .audienceType(AudienceType.PERSONAL)
                .audienceTarget(recipient)
                .destination(recipient)
                .channel(Channel.EMAIL)
                .payload(payload)
                .priority(NotificationPriority.NORMAL)
                .status(Status.PENDING)
                .createdAt(Instant.now())
                .idempotencyKey(idempotencyKey)
                .build();
        NotificationRequest request = NotificationRequest.builder()
                .recipientId(recipient)
                .payload(payload)
                .channels(List.of(Channel.EMAIL))
                .idempotencyKey(" " + idempotencyKey + " ")
                .build();
        NotificationResponse response = NotificationResponse.builder()
                .id(id)
                .recipientId(recipient)
                .channel(Channel.EMAIL)
                .payload(payload)
                .deliveryStatus(Status.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(List.of(existing));
        when(mapper.toDto(existing)).thenReturn(response);

        // When
        List<NotificationResponse> responses = service.createNotifications(request);

        // Then
        verify(repository, never()).save(any(Notification.class));
        verify(outboxRepository, never()).save(any(OutboxMessage.class));
        assertThat(responses).containsExactly(response);
    }

    @Test
    void createNotifications_ShouldReturnExistingAfterConcurrentDuplicateInsert() {
        // Given
        String idempotencyKey = "request-race";
        UUID id = UUID.randomUUID();
        Notification existing = Notification.builder()
                .id(id)
                .recipientId(recipient)
                .audienceType(AudienceType.PERSONAL)
                .audienceTarget(recipient)
                .destination(recipient)
                .channel(Channel.EMAIL)
                .payload(payload)
                .priority(NotificationPriority.NORMAL)
                .status(Status.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();
        NotificationRequest request = NotificationRequest.builder()
                .recipientId(recipient)
                .payload(payload)
                .channels(List.of(Channel.EMAIL))
                .idempotencyKey(idempotencyKey)
                .build();
        NotificationResponse response = NotificationResponse.builder()
                .id(id)
                .recipientId(recipient)
                .channel(Channel.EMAIL)
                .payload(payload)
                .deliveryStatus(Status.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        when(repository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(List.of())
                .thenReturn(List.of(existing));
        doThrow(new DataIntegrityViolationException("duplicate idempotency key"))
                .when(transactionTemplate).execute(any());
        when(mapper.toDto(existing)).thenReturn(response);

        // When
        List<NotificationResponse> responses = service.createNotifications(request);

        // Then
        assertThat(responses).containsExactly(response);
    }

    @Test
    void createNotifications_ShouldRejectSameIdempotencyKeyForDifferentPayload() {
        // Given
        String idempotencyKey = "request-conflict";
        Notification existing = Notification.builder()
                .id(UUID.randomUUID())
                .recipientId(recipient)
                .audienceType(AudienceType.PERSONAL)
                .audienceTarget(recipient)
                .destination(recipient)
                .channel(Channel.EMAIL)
                .payload("old payload")
                .priority(NotificationPriority.NORMAL)
                .status(Status.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();
        NotificationRequest request = NotificationRequest.builder()
                .recipientId(recipient)
                .payload(payload)
                .channels(List.of(Channel.EMAIL))
                .idempotencyKey(idempotencyKey)
                .build();

        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(List.of(existing));

        // When / Then
        assertThatThrownBy(() -> service.createNotifications(request))
                .isInstanceOf(DuplicateRequestConflictException.class)
                .hasMessageContaining(idempotencyKey);
        verify(repository, never()).saveAndFlush(any(Notification.class));
        verify(outboxRepository, never()).save(any(OutboxMessage.class));
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
        when(repository.findByRecipientIdAndArchivedFalseAndChannelInAndStatusIn(
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
        verify(repository).findByRecipientIdAndArchivedFalseAndChannelInAndStatusIn(
                eq(recipient), anyList(), anyList(), any(PageRequest.class));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(id);
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
        assertThat(entity.getIsRead()).isTrue();
        verify(repository).save(entity);
    }

    @Test
    void markAsRead_ShouldThrowWhenNotificationMissing() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.markAsRead(id))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining(id.toString());
        verify(repository, never()).save(any(Notification.class));
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
        assertThat(entity.getIsRead()).isFalse();
        verify(repository).save(entity);
    }

    @Test
    void markAsUnread_ShouldThrowWhenNotificationMissing() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.markAsUnread(id))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining(id.toString());
        verify(repository, never()).save(any(Notification.class));
    }
}
