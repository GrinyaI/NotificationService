package com.example.notifications.service;

import com.example.notifications.dto.PushSubscriptionRequest;
import com.example.notifications.dto.PushSubscriptionResponse;
import com.example.notifications.entity.PushSubscription;
import com.example.notifications.repository.PushSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushSubscriptionServiceTest {

    @Mock
    private PushSubscriptionRepository repository;

    private PushSubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new PushSubscriptionService(repository);
    }

    @Test
    void register_ShouldCreateSubscriptionForNewToken() {
        PushSubscriptionRequest request = PushSubscriptionRequest.builder()
                .recipientId(" user-1 ")
                .fcmToken(" token ")
                .platform("web")
                .build();
        when(repository.findByFcmToken("token")).thenReturn(Optional.empty());
        when(repository.save(any(PushSubscription.class))).thenAnswer(invocation -> {
            PushSubscription subscription = invocation.getArgument(0);
            subscription.setId(UUID.randomUUID());
            return subscription;
        });

        PushSubscriptionResponse response = service.register(request);

        assertThat(response.getRecipientId()).isEqualTo("user-1");
        assertThat(response.getPlatform()).isEqualTo("WEB");
        assertThat(response.getActive()).isTrue();
        verify(repository).save(any(PushSubscription.class));
    }

    @Test
    void register_ShouldReactivateAndUpdateExistingToken() {
        PushSubscription existing = PushSubscription.builder()
                .id(UUID.randomUUID())
                .recipientId("old-user")
                .fcmToken("token")
                .platform("WEB")
                .active(false)
                .build();
        PushSubscriptionRequest request = PushSubscriptionRequest.builder()
                .recipientId("user-1")
                .fcmToken("token")
                .platform("web")
                .build();
        when(repository.findByFcmToken("token")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        PushSubscriptionResponse response = service.register(request);

        assertThat(response.getRecipientId()).isEqualTo("user-1");
        assertThat(response.getPlatform()).isEqualTo("WEB");
        assertThat(response.getActive()).isTrue();
        assertThat(existing.getLastSeenAt()).isNotNull();
    }
}
