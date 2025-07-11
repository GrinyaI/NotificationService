package com.example.notifications.mapper;

import com.example.notifications.dto.NotificationResponse;
import com.example.notifications.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "recipientId", source = "entity.recipientId")
    @Mapping(target = "channel", source = "entity.channel")
    @Mapping(target = "payload", source = "entity.payload")
    @Mapping(target = "status", source = "entity.status")
    @Mapping(target = "createdAt", source = "entity.createdAt")
    @Mapping(target = "sentAt", source = "entity.sentAt")
    @Mapping(target = "isRead", source = "entity.isRead")
    @Mapping(target = "archived", source = "entity.archived")
    NotificationResponse toDto(Notification entity);
}
