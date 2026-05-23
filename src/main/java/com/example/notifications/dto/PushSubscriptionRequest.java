package com.example.notifications.dto;

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
    private String recipientId;

    @NotBlank
    private String fcmToken;

    @Builder.Default
    @Size(max = 30)
    private String platform = "WEB";
}
