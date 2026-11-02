package com.example.bori.domain.metric.controller;

import com.example.bori.domain.metric.controller.dto.MetricSeriesResponse;
import com.example.bori.domain.metric.controller.dto.SessionHistoryResponse;
import com.example.bori.domain.metric.controller.dto.UploadHistoryResponse;
import com.example.bori.domain.metric.service.MetricQueryService;
import com.example.bori.domain.metric.service.UsageService;
import com.example.bori.domain.user.domain.Role;
import com.example.bori.global.security.CustomUserDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cloud")
public class UsageController {

    private final MetricQueryService metricQueryService;
    private final UsageService usageService;

    // range: 300 | 900 | 1800 | 3600 | 10800 | 21600 | 43200 (진행 중 세션만)
    @GetMapping("/sessions/{sessionId}/metrics")
    public ResponseEntity<MetricSeriesResponse> getMetrics(@AuthenticationPrincipal CustomUserDetails user,
                                                           @PathVariable Long sessionId,
                                                           @RequestParam(required = false) Integer range) {
        return ResponseEntity.ok(metricQueryService.getSeries(user.getUserId(), user.getRole() == Role.ADMIN, sessionId, range));
    }

    @GetMapping("/me/sessions")
    public ResponseEntity<List<SessionHistoryResponse>> getMySessions(@AuthenticationPrincipal CustomUserDetails user,
                                                                      @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(usageService.getMySessions(user.getUserId(), days));
    }

    @GetMapping("/me/uploads")
    public ResponseEntity<List<UploadHistoryResponse>> getMyUploads(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(usageService.getMyUploads(user.getUserId()));
    }
}
