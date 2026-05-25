package com.example.notifications.entity;

import com.example.notifications.entity.enums.AudienceType;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.NotificationPriority;
import com.example.notifications.entity.enums.Status;
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
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version;

    private String recipientId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AudienceType audienceType = AudienceType.PERSONAL;

    private String audienceTarget;

    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Column(nullable = false)
    private String payload;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant sentAt;

    private Instant expiresAt;

    @Column(nullable = false, length = 128)
    private String idempotencyKey;

    private String errorDescription;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean archived = false;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (isRead == null) {
            isRead = false;
        }
        if (archived == null) {
            archived = false;
        }
        if (audienceType == null) {
            audienceType = AudienceType.PERSONAL;
        }
        if (priority == null) {
            priority = NotificationPriority.NORMAL;
        }
    }
}
