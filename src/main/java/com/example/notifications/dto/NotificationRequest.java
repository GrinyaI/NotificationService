package com.example.notifications.dto;

import com.example.notifications.entity.enums.AudienceType;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.NotificationPriority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    private String recipientId;

    @Builder.Default
    private AudienceType audienceType = AudienceType.PERSONAL;

    private String audienceTarget;

    @NotBlank
    private String payload;

    @NotEmpty
    private List<@NotNull Channel> channels;

    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Future
    private Instant expiresAt;

    @NotBlank
    @Size(max = 128)
    private String idempotencyKey;

    private Map<Channel, String> channelDestinations;

    @AssertTrue(message = "recipientId is required for PERSONAL audience")
    public boolean isRecipientValid() {
        return getResolvedAudienceType() != AudienceType.PERSONAL
                || hasText(recipientId);
    }

    @AssertTrue(message = "audienceTarget is required for SEGMENT audience")
    public boolean isAudienceTargetValid() {
        return getResolvedAudienceType() != AudienceType.SEGMENT
                || hasText(audienceTarget);
    }

    @AssertTrue(message = "channelDestinations must not contain blank destinations")
    public boolean isChannelDestinationsValid() {
        return channelDestinations == null || channelDestinations.entrySet().stream()
                .allMatch(entry -> entry.getKey() != null && hasText(entry.getValue()));
    }

    private AudienceType getResolvedAudienceType() {
        return audienceType == null ? AudienceType.PERSONAL : audienceType;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
