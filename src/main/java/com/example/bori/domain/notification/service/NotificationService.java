package com.example.bori.domain.notification.service;

import com.example.bori.domain.notification.controller.dto.NotificationListResponse;
import com.example.bori.domain.notification.controller.dto.NotificationResponse;
import com.example.bori.domain.notification.domain.Notification;
import com.example.bori.domain.notification.domain.NotificationRepository;
import com.example.bori.domain.notification.domain.NotificationType;
import com.example.bori.domain.user.domain.UserRepository;
import com.example.bori.global.push.ClientPushService;
import com.example.bori.global.push.PushType;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 알림은 DB에 남기고, 탭이 열려 있으면 WebSocket으로 바로 보낸다 (Web Push는 하지 않는다).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int LIST_SIZE = 30;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ClientPushService clientPushService;

    @Transactional
    public void notify(Long userId, NotificationType type, String message) {
        Notification notification = notificationRepository.save(
                new Notification(userRepository.getReferenceById(userId), type, message));
        clientPushService.push(userId, PushType.NOTIFICATION, NotificationResponse.from(notification));
    }

    public NotificationListResponse getMyNotifications(Long userId) {
        return new NotificationListResponse(
                notificationRepository.countByUserIdAndReadAtIsNull(userId),
                notificationRepository.findAllByUserIdOrderByIdDesc(userId, PageRequest.of(0, LIST_SIZE)).stream()
                        .map(NotificationResponse::from)
                        .toList());
    }

    @Transactional
    public void readAll(Long userId) {
        notificationRepository.markAllRead(userId, LocalDateTime.now());
    }
}
