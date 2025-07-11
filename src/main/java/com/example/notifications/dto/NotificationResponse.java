package com.example.notifications.dto;

import com.example.notifications.entity.enums.Channel;
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
    private Channel channel;
    private String payload;
    private Status deliveryStatus;
    private Instant createdAt;
    private Instant sentAt;
    private Boolean isRead;
    private Boolean archived;
}
