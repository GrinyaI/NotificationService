package com.example.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    @Schema(description = "Время возникновения ошибки.")
    private Instant timestamp;
    @Schema(description = "HTTP-статус.", example = "400")
    private int status;
    @Schema(description = "Название ошибки.", example = "Bad Request")
    private String error;
    @Schema(description = "Описание ошибки.", example = "Validation failed")
    private String message;
    @Schema(description = "Путь запроса.", example = "/api/notifications")
    private String path;
    @Schema(description = "Ошибки валидации по полям.", example = "{\"payload\":\"must not be blank\"}")
    private Map<String, String> fieldErrors;
}
