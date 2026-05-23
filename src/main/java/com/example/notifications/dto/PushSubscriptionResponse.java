package com.example.notifications.dto;

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
public class PushSubscriptionResponse {

    private UUID id;
    private String recipientId;
    private String platform;
    private Boolean active;
    private Instant createdAt;
    private Instant lastSeenAt;
}
