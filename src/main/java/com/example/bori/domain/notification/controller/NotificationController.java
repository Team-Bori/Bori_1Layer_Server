package com.example.bori.domain.notification.controller;

import com.example.bori.domain.notification.controller.dto.NotificationListResponse;
import com.example.bori.domain.notification.service.NotificationService;
import com.example.bori.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationListResponse> getMyNotifications(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(notificationService.getMyNotifications(user.getUserId()));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> readAll(@AuthenticationPrincipal CustomUserDetails user) {
        notificationService.readAll(user.getUserId());
        return ResponseEntity.noContent().build();
    }
}
