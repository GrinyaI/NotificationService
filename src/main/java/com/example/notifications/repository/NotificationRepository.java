package com.example.notifications.repository;

import com.example.notifications.entity.Notification;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByRecipientIdAndChannelInAndStatusIn(
            String recipientId,
            List<Channel> channels,
            List<Status> statuses,
            Pageable pageable
    );

    List<Notification> findAllByCreatedAtBeforeAndArchivedFalse(Instant timestamp);
}
