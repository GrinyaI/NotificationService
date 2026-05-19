package com.example.notifications.integration;

import com.example.notifications.dto.ApiErrorResponse;
import com.example.notifications.dto.NotificationRequest;
import com.example.notifications.dto.NotificationResponse;
import com.example.notifications.entity.Notification;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import com.example.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration, org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private NotificationRepository notificationRepository;

    @MockitoBean
    private RedisTemplate<?, ?> redisTemplate;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    void createAndConsumeNotification_EndToEnd() {
        NotificationRequest request = NotificationRequest.builder()
                .recipientId("integration@example.com")
                .payload("Integration Test")
                .channels(List.of(Channel.EMAIL))
                .idempotencyKey("integration-e2e")
                .build();

        ResponseEntity<NotificationResponse[]> response = restTemplate.postForEntity(
                "/api/notifications", new HttpEntity<>(request), NotificationResponse[].class);


        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        NotificationResponse[] body = response.getBody();
        assertThat(body).hasSize(1);
        UUID id = body[0].getId();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<NotificationResponse> fetched = restTemplate.getForEntity(
                    "/api/notifications/{id}", NotificationResponse.class, id);

            assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(fetched.getBody()).isNotNull();
            assertThat(fetched.getBody().getDeliveryStatus()).isEqualTo(Status.SENT);
        });
    }

    @Test
    void markRead_ShouldReturnNotFoundForMissingNotification() {
        UUID id = UUID.randomUUID();

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/notifications/{id}/read",
                HttpMethod.PATCH,
                HttpEntity.EMPTY,
                ApiErrorResponse.class,
                id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getBody().getMessage()).contains(id.toString());
    }

    @Test
    void getAll_ShouldRejectInvalidPagination() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "/api/notifications?recipientId=user@example.com&page=-1&size=0",
                ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFieldErrors()).containsKeys("page", "size");
    }

    @Test
    @Transactional
    void archiveCreatedBefore_ShouldArchiveOnlyOldActiveNotifications() {
        Instant now = Instant.now();
        Notification oldNotification = notificationRepository.save(Notification.builder()
                .recipientId("old@example.com")
                .audienceTarget("old@example.com")
                .destination("old@example.com")
                .channel(Channel.EMAIL)
                .payload("old")
                .status(Status.SENT)
                .idempotencyKey("archive-old")
                .createdAt(now.minus(31, ChronoUnit.DAYS))
                .build());
        Notification recentNotification = notificationRepository.save(Notification.builder()
                .recipientId("recent@example.com")
                .audienceTarget("recent@example.com")
                .destination("recent@example.com")
                .channel(Channel.EMAIL)
                .payload("recent")
                .status(Status.SENT)
                .idempotencyKey("archive-recent")
                .createdAt(now.minus(1, ChronoUnit.DAYS))
                .build());
        Notification alreadyArchived = notificationRepository.save(Notification.builder()
                .recipientId("archived@example.com")
                .audienceTarget("archived@example.com")
                .destination("archived@example.com")
                .channel(Channel.EMAIL)
                .payload("archived")
                .status(Status.SENT)
                .idempotencyKey("archive-already")
                .createdAt(now.minus(31, ChronoUnit.DAYS))
                .archived(true)
                .build());

        int archivedCount = notificationRepository.archiveCreatedBefore(now.minus(30, ChronoUnit.DAYS));

        assertThat(archivedCount).isEqualTo(1);
        assertThat(notificationRepository.findById(oldNotification.getId()).orElseThrow().getArchived()).isTrue();
        assertThat(notificationRepository.findById(recentNotification.getId()).orElseThrow().getArchived()).isFalse();
        assertThat(notificationRepository.findById(alreadyArchived.getId()).orElseThrow().getArchived()).isTrue();
    }

    @Test
    void create_ShouldReturnStructuredValidationErrors() {
        NotificationRequest request = NotificationRequest.builder()
                .payload("")
                .channels(List.of())
                .build();

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/notifications",
                new HttpEntity<>(request),
                ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFieldErrors()).containsKeys(
                "payload",
                "channels",
                "idempotencyKey",
                "recipientValid"
        );
    }

    @Test
    void create_ShouldRejectNullChannel() {
        NotificationRequest request = NotificationRequest.builder()
                .recipientId("user@example.com")
                .payload("payload")
                .channels(Collections.singletonList(null))
                .idempotencyKey("null-channel")
                .build();

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/notifications",
                new HttpEntity<>(request),
                ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFieldErrors()).containsKey("channels[0]");
    }

    @Test
    void create_ShouldReturnExistingNotificationForIdempotencyReplay() {
        String idempotencyKey = "replay-" + UUID.randomUUID();
        NotificationRequest request = NotificationRequest.builder()
                .recipientId("replay@example.com")
                .payload("Replay Test")
                .channels(List.of(Channel.EMAIL))
                .idempotencyKey(idempotencyKey)
                .build();

        ResponseEntity<NotificationResponse[]> firstResponse = restTemplate.postForEntity(
                "/api/notifications",
                new HttpEntity<>(request),
                NotificationResponse[].class);
        ResponseEntity<NotificationResponse[]> secondResponse = restTemplate.postForEntity(
                "/api/notifications",
                new HttpEntity<>(request),
                NotificationResponse[].class);

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(firstResponse.getBody()).hasSize(1);
        assertThat(secondResponse.getBody()).hasSize(1);
        assertThat(secondResponse.getBody()[0].getId()).isEqualTo(firstResponse.getBody()[0].getId());
        assertThat(notificationRepository.findByIdempotencyKey(idempotencyKey)).hasSize(1);
    }

    @Test
    void create_ShouldRejectIdempotencyKeyReuseWithDifferentRequest() {
        String idempotencyKey = "conflict-" + UUID.randomUUID();
        NotificationRequest firstRequest = NotificationRequest.builder()
                .recipientId("conflict@example.com")
                .payload("First")
                .channels(List.of(Channel.EMAIL))
                .idempotencyKey(idempotencyKey)
                .build();
        NotificationRequest conflictingRequest = NotificationRequest.builder()
                .recipientId("conflict@example.com")
                .payload("Second")
                .channels(List.of(Channel.EMAIL))
                .idempotencyKey(idempotencyKey)
                .build();

        restTemplate.postForEntity("/api/notifications", new HttpEntity<>(firstRequest), NotificationResponse[].class);
        ResponseEntity<ApiErrorResponse> conflictResponse = restTemplate.postForEntity(
                "/api/notifications",
                new HttpEntity<>(conflictingRequest),
                ApiErrorResponse.class);

        assertThat(conflictResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflictResponse.getBody()).isNotNull();
        assertThat(conflictResponse.getBody().getMessage()).contains(idempotencyKey);
    }
}
