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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;
    private final OutboxMessageRepository outboxRepository;
    private final NotificationMapper mapper;
    private final KafkaTopicProperties topicProperties;
    private final TransactionTemplate transactionTemplate;

    @Override
    public List<NotificationResponse> createNotifications(NotificationRequest request) {
        return findExistingNotifications(request)
                .orElseGet(() -> createNewNotifications(request));
    }

    private List<NotificationResponse> createNewNotifications(NotificationRequest request) {
        try {
            return transactionTemplate.execute(status -> createNotificationsInTransaction(request));
        } catch (DataIntegrityViolationException e) {
            return findExistingNotifications(request).orElseThrow(() -> e);
        }
    }

    private List<NotificationResponse> createNotificationsInTransaction(NotificationRequest request) {
        List<NotificationResponse> result = new ArrayList<>();
        AudienceType audienceType = resolveAudienceType(request);
        NotificationPriority priority = resolvePriority(request);
        String audienceTarget = resolveAudienceTarget(request, audienceType);
        String recipientId = normalize(request.getRecipientId());
        String idempotencyKey = requiredIdempotencyKey(request);

        for (Channel channel : uniqueChannels(request.getChannels())) {
            Notification notification = Notification.builder()
                    .recipientId(recipientId)
                    .audienceType(audienceType)
                    .audienceTarget(audienceTarget)
                    .destination(resolveDestination(request, channel))
                    .payload(request.getPayload())
                    .channel(channel)
                    .priority(priority)
                    .status(Status.PENDING)
                    .expiresAt(request.getExpiresAt())
                    .idempotencyKey(idempotencyKey)
                    .isRead(false)
                    .archived(false)
                    .build();
            notification = repository.saveAndFlush(notification);

            outboxRepository.save(buildOutboxMessage(notification));

            result.add(mapper.toDto(notification));
        }
        return result;
    }

    private OutboxMessage buildOutboxMessage(Notification notification) {
        return OutboxMessage.builder()
                .notificationId(notification.getId())
                .topic(topicProperties.topicFor(notification.getChannel()))
                .messageKey(notification.getId().toString())
                .payload(notification.getPayload())
                .status(OutboxStatus.PENDING)
                .build();
    }

    private Optional<List<NotificationResponse>> findExistingNotifications(NotificationRequest request) {
        String idempotencyKey = requiredIdempotencyKey(request);
        List<Notification> existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        List<Channel> requestedChannels = uniqueChannels(request.getChannels());
        validateIdempotentReplay(request, existing, requestedChannels, idempotencyKey);
        return Optional.of(existing.stream()
                .sorted(Comparator.comparingInt(n -> channelOrder(requestedChannels, n)))
                .map(mapper::toDto)
                .toList());
    }

    private void validateIdempotentReplay(
            NotificationRequest request,
            List<Notification> existing,
            List<Channel> requestedChannels,
            String idempotencyKey
    ) {
        Set<Channel> existingChannels = existing.stream()
                .map(Notification::getChannel)
                .collect(Collectors.toSet());
        Set<Channel> requestedChannelSet = new LinkedHashSet<>(requestedChannels);

        if (existing.size() != requestedChannels.size() || !existingChannels.equals(requestedChannelSet)) {
            throw new DuplicateRequestConflictException(idempotencyKey);
        }

        for (Notification notification : existing) {
            if (!matchesRequest(notification, request)) {
                throw new DuplicateRequestConflictException(idempotencyKey);
            }
        }
    }

    private boolean matchesRequest(Notification notification, NotificationRequest request) {
        return Objects.equals(notification.getRecipientId(), normalize(request.getRecipientId()))
                && notification.getAudienceType() == resolveAudienceType(request)
                && Objects.equals(notification.getAudienceTarget(),
                resolveAudienceTarget(request, resolveAudienceType(request)))
                && Objects.equals(notification.getDestination(), resolveDestination(request, notification.getChannel()))
                && Objects.equals(notification.getPayload(), request.getPayload())
                && notification.getPriority() == resolvePriority(request)
                && Objects.equals(notification.getExpiresAt(), request.getExpiresAt());
    }

    private int channelOrder(List<Channel> channels, Notification notification) {
        int index = channels.indexOf(notification.getChannel());
        return index < 0 ? channels.size() : index;
    }

    private List<Channel> uniqueChannels(List<Channel> channels) {
        if (channels == null) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(channels));
    }

    private AudienceType resolveAudienceType(NotificationRequest request) {
        return request.getAudienceType() == null
                ? AudienceType.PERSONAL
                : request.getAudienceType();
    }

    private NotificationPriority resolvePriority(NotificationRequest request) {
        return request.getPriority() == null
                ? NotificationPriority.NORMAL
                : request.getPriority();
    }

    private String resolveAudienceTarget(NotificationRequest request, AudienceType audienceType) {
        if (audienceType == AudienceType.PERSONAL && !hasText(request.getAudienceTarget())) {
            return normalize(request.getRecipientId());
        }
        return normalize(request.getAudienceTarget());
    }

    private String resolveDestination(NotificationRequest request, Channel channel) {
        Map<Channel, String> destinations = request.getChannelDestinations();
        if (destinations != null && destinations.containsKey(channel)) {
            return normalize(destinations.get(channel));
        }
        if (resolveAudienceType(request) == AudienceType.PERSONAL) {
            return normalize(request.getRecipientId());
        }
        return null;
    }

    private String normalize(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String requiredIdempotencyKey(NotificationRequest request) {
        String idempotencyKey = normalize(request.getIdempotencyKey());
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        return idempotencyKey;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Transactional(readOnly = true)
    @Override
    @Cacheable(cacheNames = "notification", key = "#id.toString()")
    public NotificationResponse getById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<NotificationResponse> getAll(
            String recipientId,
            List<Channel> channels,
            List<Status> statuses,
            int page,
            int size
    ) {
        Pageable sortedByDateDesc = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notification> pageEnt = getNotificationsPage(recipientId, channels, statuses, sortedByDateDesc);
        List<NotificationResponse> dtoList = pageEnt.stream()
                .map(mapper::toDto)
                .toList();

        return new PageImpl<>(
                dtoList,
                sortedByDateDesc,
                pageEnt.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    protected Page<Notification> getNotificationsPage(
            String recipientId,
            List<Channel> channels,
            List<Status> statuses,
            Pageable pageable
    ) {
        List<Channel> ch = (channels == null || channels.isEmpty())
                ? List.of(Channel.values())
                : channels;
        List<Status> st = (statuses == null || statuses.isEmpty())
                ? List.of(Status.values())
                : statuses;
        return repository.findByRecipientIdAndArchivedFalseAndChannelInAndStatusIn(recipientId, ch, st, pageable);
    }

    @Transactional
    @CacheEvict(cacheNames = "notification", key = "#id.toString()")
    @Override
    public void markAsRead(UUID id) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        notification.setIsRead(true);
        repository.save(notification);
    }

    @Transactional
    @CacheEvict(cacheNames = "notification", key = "#id.toString()")
    @Override
    public void markAsUnread(UUID id) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        notification.setIsRead(false);
        repository.save(notification);
    }
}
