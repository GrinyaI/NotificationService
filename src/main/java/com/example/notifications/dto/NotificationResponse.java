package com.example.notifications.dto;

import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.AudienceType;
import com.example.notifications.entity.enums.NotificationPriority;
import com.example.notifications.entity.enums.Status;
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
public class NotificationResponse {

    @Schema(description = "UUID уведомления.", example = "4bd32bf3-841f-441f-9bbd-83b99427c2d4")
    private UUID id;
    @Schema(description = "Идентификатор получателя.", example = "grinya")
    private String recipientId;
    @Schema(description = "Тип аудитории.", example = "PERSONAL")
    private AudienceType audienceType;
    @Schema(description = "Цель аудитории.", example = "grinya")
    private String audienceTarget;
    @Schema(description = "Фактический адрес доставки для канала.", example = "notificationservicedemo@yandex.ru")
    private String destination;
    @Schema(description = "Канал доставки.", example = "EMAIL")
    private Channel channel;
    @Schema(description = "Текст уведомления.", example = "Тестовое уведомление")
    private String payload;
    @Schema(description = "Приоритет уведомления.", example = "NORMAL")
    private NotificationPriority priority;
    @Schema(description = "Статус доставки.", example = "SENT")
    private Status deliveryStatus;
    @Schema(description = "Дата создания уведомления.")
    private Instant createdAt;
    @Schema(description = "Дата успешной отправки.")
    private Instant sentAt;
    @Schema(description = "Дата истечения уведомления.")
    private Instant expiresAt;
    @Schema(description = "Ключ идемпотентности.", example = "4bd32bf3-841f-441f-9bbd-83b99427c2d4")
    private String idempotencyKey;
    @Schema(description = "Описание ошибки доставки, если статус FAILED.", example = "Connect timed out")
    private String errorDescription;
    @Schema(description = "Флаг прочитанности.", example = "false")
    private Boolean isRead;
    @Schema(description = "Флаг архивирования.", example = "false")
    private Boolean archived;
}
