package com.example.notifications.repository;

import com.example.notifications.entity.Notification;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientIdAndArchivedFalseAndChannelInAndStatusIn(
            String recipientId,
            List<Channel> channels,
            List<Status> statuses,
            Pageable pageable
    );

    List<Notification> findByIdempotencyKey(String idempotencyKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification n
            set n.archived = true,
                n.version = n.version + 1
            where n.archived = false
              and n.createdAt < :timestamp
            """)
    int archiveCreatedBefore(@Param("timestamp") Instant timestamp);
}
