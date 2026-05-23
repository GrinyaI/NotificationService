package com.example.notifications.repository;

import com.example.notifications.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {

    Optional<PushSubscription> findByFcmToken(String fcmToken);

    List<PushSubscription> findByRecipientIdAndActiveTrue(String recipientId);
}
