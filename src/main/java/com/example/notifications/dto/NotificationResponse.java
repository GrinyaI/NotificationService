package com.example.notifications.dto;

import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.AudienceType;
import com.example.notifications.entity.enums.NotificationPriority;
import com.example.notifications.entity.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID id;
    private String recipientId;
    private AudienceType audienceType;
    private String audienceTarget;
    private String destination;
    private Channel channel;
    private String payload;
    private NotificationPriority priority;
    private Status deliveryStatus;
    private Instant createdAt;
    private Instant sentAt;
    private Instant expiresAt;
    private String idempotencyKey;
    private String errorDescription;
    private Boolean isRead;
    private Boolean archived;
}
