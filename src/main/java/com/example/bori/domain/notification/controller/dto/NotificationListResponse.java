package com.example.bori.domain.notification.controller.dto;

import java.util.List;

public record NotificationListResponse(
        long unreadCount,
        List<NotificationResponse> notifications
) {
}
