package com.example.notifications.repository;

import com.example.notifications.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {

    @Query(value = """
            SELECT *
            FROM notification_outbox
            WHERE status = 'PENDING'
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxMessage> findPendingForUpdate(@Param("limit") int limit);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboxMessage m
            set m.status = com.example.notifications.entity.enums.OutboxStatus.PENDING,
                m.version = m.version + 1
            where m.status = com.example.notifications.entity.enums.OutboxStatus.PROCESSING
              and m.lastAttemptAt < :threshold
            """)
    int resetStaleProcessing(@Param("threshold") Instant threshold);
}
