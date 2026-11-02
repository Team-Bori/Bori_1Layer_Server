package com.example.bori.domain.session.controller;

import com.example.bori.domain.session.controller.dto.CloudMeResponse;
import com.example.bori.domain.session.controller.dto.MySessionResponse;
import com.example.bori.domain.session.controller.dto.SessionStartRequest;
import com.example.bori.domain.session.service.CloudSessionService;
import com.example.bori.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cloud")
public class CloudSessionController {

    private final CloudSessionService cloudSessionService;

    @GetMapping("/me")
    public ResponseEntity<CloudMeResponse> getMe(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(cloudSessionService.getMe(user.getUserId()));
    }

    @PostMapping("/boards/{boardId}/sessions")
    public ResponseEntity<MySessionResponse> start(@AuthenticationPrincipal CustomUserDetails user,
                                                   @PathVariable Long boardId,
                                                   @Valid @RequestBody SessionStartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cloudSessionService.start(user.getUserId(), boardId, request));
    }

    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<Void> end(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long sessionId) {
        cloudSessionService.end(user.getUserId(), sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{sessionId}/activity")
    public ResponseEntity<Void> activity(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long sessionId) {
        cloudSessionService.activity(user.getUserId(), sessionId);
        return ResponseEntity.noContent().build();
    }
}
