package com.example.notifications.controller;

import com.example.notifications.dto.NotificationRequest;
import com.example.notifications.dto.NotificationResponse;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import com.example.notifications.mapper.NotificationMapper;
import com.example.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;
    private final NotificationMapper mapper;

    @Operation(
            summary = "Создать уведомления",
            description = "Принимает сообщение и список каналов, создаёт уведомление в БД и публикует в Kafka для каждого канала.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "DTO с данными уведомления",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NotificationRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Уведомления успешно созданы",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = NotificationResponse.class)))),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации запроса",
                            content = @Content(schema = @Schema()))
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
            description = "Возвращает всю информацию по конкретному уведомлению.",
            parameters = {
                    @Parameter(name = "id", description = "UUID уведомления", required = true, in = ParameterIn.PATH)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Уведомление найдено",
                            content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Уведомление не найдено",
                            content = @Content)
            }
    )
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public NotificationResponse getById(@PathVariable UUID id) {
        return service.getById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    @Operation(
            summary = "Список уведомлений пользователя",
            description = "Возвращает постраничный список уведомлений для указанного получателя с возможностью фильтрации по каналам и статусам.",
            parameters = {
                    @Parameter(name = "recipientId", description = "Идентификатор получателя (email, телефон и т.п.)", required = true, in = ParameterIn.QUERY),
                    @Parameter(name = "channels", description = "Список каналов для фильтрации", in = ParameterIn.QUERY,
                            array = @ArraySchema(schema = @Schema(implementation = Channel.class))),
                    @Parameter(name = "statuses", description = "Список статусов для фильтрации", in = ParameterIn.QUERY,
                            array = @ArraySchema(schema = @Schema(implementation = Status.class))),
                    @Parameter(name = "page", description = "Номер страницы (начиная с 0)", in = ParameterIn.QUERY),
                    @Parameter(name = "size", description = "Размер страницы", in = ParameterIn.QUERY)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Страница уведомлений",
                            content = @Content(schema = @Schema(implementation = Page.class)))
            }
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<NotificationResponse>> getAll(
            @RequestParam String recipientId,
            @RequestParam(required = false) List<Channel> channels,
            @RequestParam(required = false) List<Status> statuses,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.getAll(recipientId, channels, statuses, page, size));
    }

    @Operation(
            summary = "Отметить уведомление как прочитанное",
            description = "Меняет флаг isRead на true.",
            parameters = @Parameter(name = "id", description = "UUID уведомления", required = true, in = ParameterIn.PATH),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Уведомление отмечено как прочитанное"),
                    @ApiResponse(responseCode = "404", description = "Уведомление не найдено")
            }
    )
    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID id) {
        service.markAsRead(id);
    }

    @Operation(
            summary = "Отметить уведомление как непрочитанное",
            description = "Меняет флаг isRead на false.",
            parameters = @Parameter(name = "id", description = "UUID уведомления", required = true, in = ParameterIn.PATH),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Уведомление отмечено как непрочитанное"),
                    @ApiResponse(responseCode = "404", description = "Уведомление не найдено")
            }
    )
    @PatchMapping("/{id}/unread")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markUnread(@PathVariable UUID id) {
        service.markAsUnread(id);
    }
}