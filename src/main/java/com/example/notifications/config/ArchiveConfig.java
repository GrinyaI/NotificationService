package com.example.notifications.config;

import com.example.notifications.entity.Notification;
import com.example.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ArchiveConfig {
    private final NotificationRepository repository;

    @Scheduled(cron = "0 0 3 * * ?")
    public void archiveOldNotifications() {
        Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Notification> old = repository.findAllByCreatedAtBeforeAndArchivedFalse(threshold);
        old.forEach(n -> n.setArchived(true));
        repository.saveAll(old);
    }
}
