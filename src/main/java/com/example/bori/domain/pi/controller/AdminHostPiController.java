package com.example.bori.domain.pi.controller;

import com.example.bori.domain.pi.controller.dto.HostPiCreateRequest;
import com.example.bori.domain.pi.controller.dto.HostPiResponse;
import com.example.bori.domain.pi.controller.dto.HostPiTokenResponse;
import com.example.bori.domain.pi.service.HostPiService;
import com.example.bori.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/admin/pis")
public class AdminHostPiController {

    private final HostPiService hostPiService;

    @GetMapping
    public ResponseEntity<List<HostPiResponse>> getPis() {
        return ResponseEntity.ok(hostPiService.getPis());
    }

    @PostMapping
    public ResponseEntity<HostPiTokenResponse> create(@AuthenticationPrincipal CustomUserDetails admin,
                                                      @Valid @RequestBody HostPiCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hostPiService.create(admin.getUserId(), request));
    }

    @PostMapping("/{piId}/token")
    public ResponseEntity<HostPiTokenResponse> reissueToken(@AuthenticationPrincipal CustomUserDetails admin,
                                                            @PathVariable Long piId) {
        return ResponseEntity.ok(hostPiService.reissueToken(admin.getUserId(), piId));
    }
}
