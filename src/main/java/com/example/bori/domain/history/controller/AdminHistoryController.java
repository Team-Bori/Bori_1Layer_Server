package com.example.bori.domain.history.controller;

import com.example.bori.domain.history.controller.dto.AdminLogResponse;
import com.example.bori.domain.history.controller.dto.SessionLogResponse;
import com.example.bori.domain.history.service.HistoryService;
import com.example.bori.domain.rental.controller.dto.AdminRentalResponse;
import com.example.bori.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 이력 조회 탭. q: 사용자 이름·학번·보드명(조작 로그는 관리자 이름·내용) 부분 검색
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/history")
public class AdminHistoryController {

    private final HistoryService historyService;

    @GetMapping("/sessions")
    public ResponseEntity<PageResponse<SessionLogResponse>> getSessions(@RequestParam(required = false) String q,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(historyService.getSessions(q, page, size));
    }

    @GetMapping("/rentals")
    public ResponseEntity<PageResponse<AdminRentalResponse>> getRentals(@RequestParam(required = false) String q,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(historyService.getRentals(q, page, size));
    }

    @GetMapping("/logs")
    public ResponseEntity<PageResponse<AdminLogResponse>> getAdminLogs(@RequestParam(required = false) String q,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(historyService.getAdminLogs(q, page, size));
    }
}
