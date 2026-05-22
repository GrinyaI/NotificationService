package com.example.notifications.service.delivery;

import com.example.notifications.config.ExolveProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExolveClientTest {

    @Test
    void send_ShouldPostJsonRequestToExolve() {
        ExolveProperties properties = new ExolveProperties();
        properties.setBaseUrl("https://api.exolve.ru");
        properties.setApiKey("api-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExolveClient client = new ExolveClient(builder, properties);

        server.expect(requestTo("https://api.exolve.ru/messaging/v1/SendSMS"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer api-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "number": "79991112233",
                          "destination": "79992223344",
                          "text": "Test message"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "message_id": "439166538239448536",
                          "template_resource_id": 136519
                        }
                        """, MediaType.APPLICATION_JSON));

        ExolveSendSmsResponse response = client.send("79991112233", "79992223344", "Test message");

        assertThat(response.getMessageId()).isEqualTo("439166538239448536");
        assertThat(response.getTemplateResourceId()).isEqualTo(136519);
        server.verify();
    }
}
