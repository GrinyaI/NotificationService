package com.example.notifications.controller;

import com.example.notifications.dto.NotificationRequest;
import com.example.notifications.dto.NotificationResponse;
import com.example.notifications.entity.Notification;
import com.example.notifications.entity.enums.Channel;
import com.example.notifications.entity.enums.Status;
import com.example.notifications.mapper.NotificationMapper;
import com.example.notifications.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;
    private final NotificationMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<NotificationResponse> create(
            @Valid @RequestBody NotificationRequest request
    ) {
        return service.createNotifications(request);
    }

    @GetMapping("/{id}")
    public NotificationResponse getById(@PathVariable UUID id) {
        return service.getById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getAll(
            @RequestParam String recipientId,
            @RequestParam(required = false) List<Channel> channels,
            @RequestParam(required = false) List<Status> statuses,
            Pageable pageable
    ) {
        List<Notification> list = service.getNotificationsList(recipientId, channels, statuses, pageable);

        long total = service.getNotificationsPage(recipientId, channels, statuses, pageable).getTotalElements();

        List<NotificationResponse> dtoList = list.stream()
                .map(mapper::toDto)
                .toList();
        Page<NotificationResponse> page = new PageImpl<>(dtoList, pageable, total);

        return ResponseEntity.ok(page);
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID id) {
        service.markAsRead(id);
    }

    @PatchMapping("/{id}/unread")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markUnread(@PathVariable UUID id) {
        service.markAsUnread(id);
    }
}