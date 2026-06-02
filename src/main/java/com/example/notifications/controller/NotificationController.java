package com.example.notifications.controller;

import com.example.notifications.dto.ApiErrorResponse;
import com.example.notifications.dto.NotificationRequest;
import com.example.notifications.dto.NotificationResponse;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import com.example.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService service;

    @Operation(
            summary = "Создать уведомления",
            description = "Создает по одному уведомлению на каждый канал и ставит их в outbox для асинхронной "
                    + "публикации в Kafka. idempotencyKey обязателен и защищает повторные запросы от дублей.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные уведомления",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NotificationRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Email notification",
                                            value = """
                                                    {
                                                      "recipientId": "grinya",
                                                      "audienceType": "PERSONAL",
                                                      "audienceTarget": "grinya",
                                                      "payload": "Тестовое уведомление",
                                                      "channels": ["EMAIL"],
                                                      "priority": "NORMAL",
                                                      "idempotencyKey": "email-demo-4bd32bf3-841f-441f-9bbd-83b99427c2d4",
                                                      "channelDestinations": {
                                                        "EMAIL": "notificationservicedemo@yandex.ru"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "All channels",
                                            value = """
                                                    {
                                                      "recipientId": "demo-user",
                                                      "audienceType": "PERSONAL",
                                                      "audienceTarget": "demo-user",
                                                      "payload": "Тестовое уведомление",
                                                      "channels": ["SMS", "EMAIL", "PUSH"],
                                                      "priority": "NORMAL",
                                                      "idempotencyKey": "all-channels-4bd32bf3-841f-441f-9bbd-83b99427c2d4",
                                                      "channelDestinations": {
                                                        "SMS": "79048269449",
                                                        "EMAIL": "notificationservicedemo@yandex.ru"
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Уведомления созданы",
                            content = @Content(array = @ArraySchema(schema = @Schema(
                                    implementation = NotificationResponse.class)))),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации запроса",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "idempotencyKey уже использован другим запросом",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<NotificationResponse> create(
            @Valid @RequestBody NotificationRequest request
    ) {
        return service.createNotifications(request);
    }

    @Operation(
            summary = "Получить уведомление по ID",
            description = "Возвращает данные конкретного уведомления.",
            parameters = {
                    @Parameter(name = "id", description = "UUID уведомления", required = true,
                            in = ParameterIn.PATH)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Уведомление найдено",
                            content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Уведомление не найдено",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public NotificationResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @Operation(
            summary = "Список уведомлений пользователя",
            description = "Возвращает страницу активных уведомлений получателя с фильтрацией по каналам и статусам.",
            parameters = {
                    @Parameter(name = "recipientId", description = "Идентификатор получателя", required = true,
                            in = ParameterIn.QUERY),
                    @Parameter(name = "channels", description = "Каналы для фильтрации", in = ParameterIn.QUERY,
                            array = @ArraySchema(schema = @Schema(implementation = Channel.class))),
                    @Parameter(name = "statuses", description = "Статусы для фильтрации", in = ParameterIn.QUERY,
                            array = @ArraySchema(schema = @Schema(implementation = Status.class))),
                    @Parameter(name = "page", description = "Номер страницы, начиная с 0", in = ParameterIn.QUERY),
                    @Parameter(name = "size", description = "Размер страницы от 1 до 100", in = ParameterIn.QUERY)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Страница уведомлений",
                            content = @Content(schema = @Schema(implementation = Page.class))),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации запроса",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<NotificationResponse>> getAll(
            @NotBlank @RequestParam String recipientId,
            @RequestParam(required = false) List<Channel> channels,
            @RequestParam(required = false) List<Status> statuses,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.getAll(recipientId, channels, statuses, page, size));
    }

    @Operation(
            summary = "Отметить как прочитанное",
            description = "Меняет флаг isRead на true.",
            parameters = @Parameter(name = "id", description = "UUID уведомления", required = true,
                    in = ParameterIn.PATH),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Уведомление отмечено как прочитанное"),
                    @ApiResponse(responseCode = "404", description = "Уведомление не найдено",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID id) {
        service.markAsRead(id);
    }

    @Operation(
            summary = "Отметить как непрочитанное",
            description = "Меняет флаг isRead на false.",
            parameters = @Parameter(name = "id", description = "UUID уведомления", required = true,
                    in = ParameterIn.PATH),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Уведомление отмечено как непрочитанное"),
                    @ApiResponse(responseCode = "404", description = "Уведомление не найдено",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @PatchMapping("/{id}/unread")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markUnread(@PathVariable UUID id) {
        service.markAsUnread(id);
    }
}
