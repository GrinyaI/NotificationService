package com.example.notifications.service.delivery;

import com.example.notifications.config.ExolveProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExolveClient {

    private static final String SEND_SMS_PATH = "/messaging/v1/SendSMS";

    private final RestClient.Builder restClientBuilder;
    private final ExolveProperties properties;

    public ExolveSendSmsResponse send(String from, String to, String text) {
        try {
            ExolveSendSmsResponse response = restClientBuilder.clone()
                    .baseUrl(properties.getBaseUrl())
                    .build()
                    .post()
                    .uri(SEND_SMS_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "number", from,
                            "destination", to,
                            "text", text
                    ))
                    .retrieve()
                    .body(ExolveSendSmsResponse.class);
            if (response == null || !StringUtils.hasText(response.getMessageId())) {
                throw new SmsDeliveryException("MTS Exolve returned empty message_id");
            }
            return response;
        } catch (RestClientResponseException e) {
            throw new SmsDeliveryException("MTS Exolve request failed: "
                    + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new SmsDeliveryException("MTS Exolve request failed", e);
        }
    }
}
