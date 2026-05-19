package com.example.notifications.config;

import com.example.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArchiveConfigTest {

    @Mock
    private NotificationRepository repository;

    @Test
    void archiveOldNotifications_ShouldArchiveNotificationsOlderThanRetention() {
        ArchiveConfig archiveConfig = new ArchiveConfig(repository);
        ReflectionTestUtils.setField(archiveConfig, "retentionDays", 7L);
        Instant earliestExpected = Instant.now().minus(7, ChronoUnit.DAYS).minusSeconds(1);

        archiveConfig.archiveOldNotifications();

        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).archiveCreatedBefore(thresholdCaptor.capture());
        Instant latestExpected = Instant.now().minus(7, ChronoUnit.DAYS).plusSeconds(1);
        assertThat(thresholdCaptor.getValue())
                .isAfterOrEqualTo(earliestExpected)
                .isBeforeOrEqualTo(latestExpected);
    }
}
