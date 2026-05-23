package com.example.notifications.service;

import com.example.notifications.dto.PushSubscriptionRequest;
import com.example.notifications.dto.PushSubscriptionResponse;
import com.example.notifications.entity.PushSubscription;
import com.example.notifications.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

    private final PushSubscriptionRepository repository;

    @Transactional
    public PushSubscriptionResponse register(PushSubscriptionRequest request) {
        String token = normalize(request.getFcmToken());
        String recipientId = normalize(request.getRecipientId());
        String platform = normalizePlatform(request.getPlatform());
        Instant now = Instant.now();

        PushSubscription subscription = repository.findByFcmToken(token)
                .map(existing -> updateExisting(existing, recipientId, platform, now))
                .orElseGet(() -> createNew(recipientId, token, platform, now));

        return toResponse(repository.save(subscription));
    }

    private PushSubscription updateExisting(
            PushSubscription subscription,
            String recipientId,
            String platform,
            Instant now
    ) {
        subscription.setRecipientId(recipientId);
        subscription.setPlatform(platform);
        subscription.setActive(true);
        subscription.setLastSeenAt(now);
        return subscription;
    }

    private PushSubscription createNew(String recipientId, String token, String platform, Instant now) {
        return PushSubscription.builder()
                .recipientId(recipientId)
                .fcmToken(token)
                .platform(platform)
                .active(true)
                .createdAt(now)
                .lastSeenAt(now)
                .build();
    }

    private PushSubscriptionResponse toResponse(PushSubscription subscription) {
        return PushSubscriptionResponse.builder()
                .id(subscription.getId())
                .recipientId(subscription.getRecipientId())
                .platform(subscription.getPlatform())
                .active(subscription.getActive())
                .createdAt(subscription.getCreatedAt())
                .lastSeenAt(subscription.getLastSeenAt())
                .build();
    }

    private String normalizePlatform(String platform) {
        String normalized = normalize(platform);
        return normalized == null ? "WEB" : normalized.toUpperCase();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
