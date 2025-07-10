package com.example.notifications.dto;

import com.example.notifications.entity.enums.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    @NotBlank
    private String recipientId;

    @NotBlank
    private String payload;

    @NotEmpty
    private List<Channel> channels;
}
