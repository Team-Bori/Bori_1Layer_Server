package com.example.bori.domain.queue.controller;

import com.example.bori.domain.queue.controller.dto.MyQueueResponse;
import com.example.bori.domain.queue.service.QueueService;
import com.example.bori.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cloud")
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/boards/{boardId}/queue")
    public ResponseEntity<MyQueueResponse> join(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long boardId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(queueService.join(user.getUserId(), boardId));
    }

    @DeleteMapping("/queue")
    public ResponseEntity<Void> leave(@AuthenticationPrincipal CustomUserDetails user) {
        queueService.leave(user.getUserId());
        return ResponseEntity.noContent().build();
    }
}
