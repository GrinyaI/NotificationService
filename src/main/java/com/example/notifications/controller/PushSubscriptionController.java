package com.example.notifications.controller;

import com.example.notifications.dto.ApiErrorResponse;
import com.example.notifications.dto.PushSubscriptionRequest;
import com.example.notifications.dto.PushSubscriptionResponse;
import com.example.notifications.service.PushSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push/subscriptions")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final PushSubscriptionService service;

    @Operation(
            summary = "Зарегистрировать push-подписку",
            description = "Сохраняет или обновляет FCM token получателя для отправки браузерных push-уведомлений.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные push-подписки",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PushSubscriptionRequest.class),
                            examples = @ExampleObject(
                                    name = "Web push subscription",
                                    value = """
                                            {
                                              "recipientId": "demo-user",
                                              "fcmToken": "fcm-token-from-demo-frontend",
                                              "platform": "WEB"
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Push-подписка зарегистрирована",
                            content = @Content(schema = @Schema(implementation = PushSubscriptionResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации запроса",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PushSubscriptionResponse register(@Valid @RequestBody PushSubscriptionRequest request) {
        return service.register(request);
    }
}
