package com.example.bori.domain.notification.controller.dto;

import com.example.bori.domain.notification.domain.Notification;
import com.example.bori.domain.notification.domain.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        boolean read,
        LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getReadAt() != null, n.getCreatedAt());
    }
}
