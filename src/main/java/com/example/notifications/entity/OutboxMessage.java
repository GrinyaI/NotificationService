package com.example.notifications.entity;

import com.example.notifications.entity.enums.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxMessage {

    @Id
    @GeneratedValue
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private UUID notificationId;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String messageKey;

    @Column(nullable = false)
    private String payload;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant lastAttemptAt;

    private Instant publishedAt;

    private String lastError;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = OutboxStatus.PENDING;
        }
        if (attempts == null) {
            attempts = 0;
        }
    }
}
