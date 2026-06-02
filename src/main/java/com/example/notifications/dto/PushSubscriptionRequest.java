package com.example.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushSubscriptionRequest {

    @NotBlank
    @Schema(description = "Идентификатор получателя push-уведомлений.", example = "demo-user")
    private String recipientId;

    @NotBlank
    @Schema(description = "FCM token браузера или устройства.", example = "fcm-token-from-demo-frontend")
    private String fcmToken;

    @Builder.Default
    @Size(max = 30)
    @Schema(description = "Платформа подписки.", example = "WEB")
    private String platform = "WEB";
}
