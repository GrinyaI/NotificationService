package com.example.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "UUID push-подписки.", example = "4bd32bf3-841f-441f-9bbd-83b99427c2d4")
    private UUID id;
    @Schema(description = "Идентификатор получателя.", example = "demo-user")
    private String recipientId;
    @Schema(description = "Платформа подписки.", example = "WEB")
    private String platform;
    @Schema(description = "Активна ли подписка.", example = "true")
    private Boolean active;
    @Schema(description = "Дата создания подписки.")
    private Instant createdAt;
    @Schema(description = "Дата последнего обновления подписки.")
    private Instant lastSeenAt;
}
