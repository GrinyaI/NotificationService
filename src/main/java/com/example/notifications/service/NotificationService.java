package com.example.notifications.service;

import com.example.notifications.dto.NotificationRequest;
import com.example.notifications.dto.NotificationResponse;
import com.example.notifications.entity.Notification;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationService {
    List<NotificationResponse> createNotifications(NotificationRequest request);

    Optional<Notification> getById(UUID id);

    Page<NotificationResponse> getAll(
            String recipientId,
            List<Channel> channels,
            List<Status> statuses,
            int page,
            int size
    );

    void markAsRead(UUID id);

    void markAsUnread(UUID id);
}
