package com.example.notifications.config;

import com.example.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Configuration
@RequiredArgsConstructor
public class ArchiveConfig {

    private final NotificationRepository repository;

    @Value("${notification.archive.retention-days:30}")
    private long retentionDays = 30;

    @Transactional
    @Scheduled(cron = "${notification.archive.cron:0 0 3 * * ?}")
    public void archiveOldNotifications() {
        Instant threshold = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        repository.archiveCreatedBefore(threshold);
    }
}
