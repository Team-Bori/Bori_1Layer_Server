package com.example.bori.domain.auth.controller;

import com.example.bori.domain.auth.controller.dto.LoginRequest;
import com.example.bori.domain.auth.controller.dto.SignUpRequest;
import com.example.bori.domain.auth.service.AuthService;
import com.example.bori.domain.user.controller.dto.UserInfoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserInfoResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserInfoResponse> login(@Valid @RequestBody LoginRequest request,
                                                  HttpServletRequest httpRequest,
                                                  HttpServletResponse httpResponse) {
        return ResponseEntity.ok(authService.login(request, httpRequest, httpResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(httpRequest, httpResponse);
        return ResponseEntity.noContent().build();
    }
}
