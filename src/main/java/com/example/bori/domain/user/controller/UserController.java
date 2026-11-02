package com.example.bori.domain.user.controller;

import com.example.bori.domain.user.controller.dto.UserInfoResponse;
import com.example.bori.domain.user.service.UserService;
import com.example.bori.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getUserInfo(userDetails.getUserId()));
    }

    @PatchMapping("/me/notify")
    public ResponseEntity<UserInfoResponse> changeNotify(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                         @Valid @RequestBody NotifyRequest request) {
        return ResponseEntity.ok(userService.changeNotifyEnabled(userDetails.getUserId(), request.enabled()));
    }

    public record NotifyRequest(@NotNull(message = "enabled를 입력해주세요.") Boolean enabled) {
    }
}
