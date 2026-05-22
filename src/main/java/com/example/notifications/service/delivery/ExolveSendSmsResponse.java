package com.example.notifications.service.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ExolveSendSmsResponse {

    @JsonProperty("message_id")
    private String messageId;
    @JsonProperty("template_resource_id")
    private Long templateResourceId;
}
