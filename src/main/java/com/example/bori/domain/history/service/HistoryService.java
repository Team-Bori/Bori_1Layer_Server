package com.example.bori.domain.history.service;

import com.example.bori.domain.adminlog.domain.AdminActionLogRepository;
import com.example.bori.domain.history.controller.dto.AdminLogResponse;
import com.example.bori.domain.history.controller.dto.SessionLogResponse;
import com.example.bori.domain.rental.controller.dto.AdminRentalResponse;
import com.example.bori.domain.rental.domain.RentalRepository;
import com.example.bori.domain.session.domain.BoardSession;
import com.example.bori.domain.session.domain.BoardSessionRepository;
import com.example.bori.global.common.PageResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryService {

    private static final int MAX_SIZE = 100;

    private final BoardSessionRepository boardSessionRepository;
    private final RentalRepository rentalRepository;
    private final AdminActionLogRepository adminActionLogRepository;

    public PageResponse<SessionLogResponse> getSessions(String q, int page, int size) {
        LocalDateTime now = LocalDateTime.now();
        return PageResponse.of(boardSessionRepository.search(keyword(q), pageable(page, size)), s -> toSessionLog(s, now));
    }

    public PageResponse<AdminRentalResponse> getRentals(String q, int page, int size) {
        return PageResponse.of(rentalRepository.search(keyword(q), pageable(page, size)), AdminRentalResponse::from);
    }

    public PageResponse<AdminLogResponse> getAdminLogs(String q, int page, int size) {
        return PageResponse.of(adminActionLogRepository.search(keyword(q), pageable(page, size)), AdminLogResponse::from);
    }

    private SessionLogResponse toSessionLog(BoardSession s, LocalDateTime now) {
        var board = s.getCloudBoard();
        return new SessionLogResponse(s.getId(), s.getStartedAt(), s.getEndedAt(), s.getUser().getName(),
                s.getUser().getStudentNo(), board.getId(), board.getName(), board.isDeleted(), s.modelLabel(),
                s.durationSec(now), s.getEndReason());
    }

    private String keyword(String q) {
        return q == null ? "" : q.trim();
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_SIZE));
    }
}
