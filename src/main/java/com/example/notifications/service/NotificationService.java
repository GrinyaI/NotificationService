package com.example.notifications.service;


import com.example.notifications.dto.NotificationRequest;
import com.example.notifications.dto.NotificationResponse;
import com.example.notifications.entity.Notification;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import com.example.notifications.mapper.NotificationMapper;
import com.example.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repository;
    private final NotificationMapper mapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    @CacheEvict(value = "notifications", key = "#request.recipientId + '-*'", allEntries = true)
    public List<NotificationResponse> createNotifications(NotificationRequest request) {
        List<NotificationResponse> result = new ArrayList<>();
        for (Channel channel : request.getChannels()) {
            Notification notification = Notification.builder()
                    .recipientId(request.getRecipientId())
                    .payload(request.getPayload())
                    .channel(channel)
                    .status(Status.PENDING)
                    .createdAt(Instant.now())
                    .isRead(false)
                    .archived(false)
                    .build();
            notification = repository.save(notification);

            String topic = getTopicByChannel(channel);
            kafkaTemplate.send(topic, notification.getId().toString(), notification.getPayload());

            result.add(mapper.toDto(notification));
        }
        return result;
    }

    private String getTopicByChannel(Channel channel) {
        return switch (channel) {
            case EMAIL -> "notifications.email";
            case SMS -> "notifications.sms";
            case PUSH -> "notifications.push";
        };
    }

    @Transactional(readOnly = true)
    public Optional<Notification> getById(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
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
        return repository.findByRecipientIdAndChannelInAndStatusIn(recipientId, ch, st, pageable);
    }

    @Transactional
    public void markAsRead(UUID id) {
        repository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            repository.save(n);
        });
    }

    @Transactional
    public void markAsUnread(UUID id) {
        repository.findById(id).ifPresent(n -> {
            n.setIsRead(false);
            repository.save(n);
        });
    }
}
