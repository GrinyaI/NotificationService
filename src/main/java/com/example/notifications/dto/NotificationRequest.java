package com.example.notifications.dto;

import com.example.notifications.entity.enums.AudienceType;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.NotificationPriority;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "Идентификатор получателя для PERSONAL-рассылки.", example = "grinya")
    private String recipientId;

    @Builder.Default
    @Schema(description = "Тип аудитории.", example = "PERSONAL")
    private AudienceType audienceType = AudienceType.PERSONAL;

    @Schema(description = "Цель аудитории. Для PERSONAL обычно совпадает с recipientId.", example = "grinya")
    private String audienceTarget;

    @NotBlank
    @Schema(description = "Текст уведомления.", example = "Тестовое уведомление")
    private String payload;

    @NotEmpty
    @Schema(description = "Каналы доставки.", example = "[\"EMAIL\"]")
    private List<@NotNull Channel> channels;

    @Builder.Default
    @Schema(description = "Приоритет уведомления.", example = "NORMAL")
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Future
    @Schema(description = "Опциональная дата истечения. Значение должно быть в будущем.",
            example = "2027-01-01T00:00:00Z")
    private Instant expiresAt;

    @NotBlank
    @Size(max = 128)
    @Schema(description = "Ключ идемпотентности для защиты от повторного создания уведомлений.",
            example = "4bd32bf3-841f-441f-9bbd-83b99427c2d4")
    private String idempotencyKey;

    @Schema(description = "Опциональные адреса доставки по каналам.",
            example = "{\"EMAIL\":\"notificationservicedemo@yandex.ru\",\"SMS\":\"79048269449\"}")
    private Map<Channel, String> channelDestinations;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "recipientId is required for PERSONAL audience")
    public boolean isRecipientValid() {
        return getResolvedAudienceType() != AudienceType.PERSONAL
                || hasText(recipientId);
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "audienceTarget is required for SEGMENT audience")
    public boolean isAudienceTargetValid() {
        return getResolvedAudienceType() != AudienceType.SEGMENT
                || hasText(audienceTarget);
    }

    @JsonIgnore
    @Schema(hidden = true)
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
