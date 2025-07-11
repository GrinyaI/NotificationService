package com.example.notifications.integration;

import com.example.notifications.dto.NotificationRequest;
import com.example.notifications.dto.NotificationResponse;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration, org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private RedisTemplate<?, ?> redisTemplate;

    @AfterAll
    static void stopContainers() {
        postgres.stop();
        kafka.stop();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        postgres.start();
        kafka.start();

        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
        registry.add("spring.kafka.bootstrap-servers", () -> kafka.getBootstrapServers());
    }

    @Test
    void createAndConsumeNotification_EndToEnd() throws InterruptedException {
        NotificationRequest request = NotificationRequest.builder()
                .recipientId("integration@example.com")
                .payload("Integration Test")
                .channels(List.of(Channel.EMAIL))
                .build();

        ResponseEntity<NotificationResponse[]> response = restTemplate.postForEntity(
                "/api/notifications", new HttpEntity<>(request), NotificationResponse[].class);


        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        NotificationResponse[] body = response.getBody();
        assertThat(body).hasSize(1);
        UUID id = body[0].getId();

        Thread.sleep(2000);

        ResponseEntity<NotificationResponse> fetched = restTemplate.getForEntity(
                "/api/notifications/{id}", NotificationResponse.class, id);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().getDeliveryStatus()).isEqualTo(Status.SENT);
    }
}