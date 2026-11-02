package com.example.bori.domain.metric.service;

import com.example.bori.domain.board.domain.CloudBoard;
import com.example.bori.domain.metric.controller.dto.SessionHistoryResponse;
import com.example.bori.domain.metric.controller.dto.UploadHistoryResponse;
import com.example.bori.domain.session.domain.BoardSession;
import com.example.bori.domain.session.domain.BoardSessionRepository;
import com.example.bori.domain.session.domain.UserModelUploadRepository;
import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsageService {

    private final BoardSessionRepository boardSessionRepository;
    private final UserModelUploadRepository uploadRepository;
    private final NamedParameterJdbcTemplate jdbc;

    // 최근 days일(7 또는 30)에 시작한 내 세션. 사용 시간·보드별·종료 사유 집계는 이 목록으로 계산한다.
    public List<SessionHistoryResponse> getMySessions(Long userId, int days) {
        if (days != 7 && days != 30) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        LocalDateTime now = LocalDateTime.now();
        List<BoardSession> sessions = boardSessionRepository.findAllByUserIdAndStartedAtAfterOrderByIdDesc(userId, now.minusDays(days));
        Map<Long, Double> avgFps = averageFps(sessions.stream().map(BoardSession::getId).toList());
        return sessions.stream().map(s -> {
            CloudBoard board = s.getCloudBoard();
            return new SessionHistoryResponse(s.getId(), s.getStartedAt(), s.getEndedAt(), board.getId(), board.getName(),
                    board.isDeleted(), s.modelLabel(), s.durationSec(now), avgFps.get(s.getId()), s.getEndReason());
        }).toList();
    }

    public List<UploadHistoryResponse> getMyUploads(Long userId) {
        return uploadRepository.findAllByUserIdOrderByIdDesc(userId).stream()
                .map(u -> new UploadHistoryResponse(u.getId(), u.getGithubUrl(), u.getModelFormat(), u.getSizeMb(),
                        u.getCloudBoard().getId(), u.getCloudBoard().getName(), u.getCloudBoard().getUploadLimitMb(),
                        u.getCreatedAt()))
                .toList();
    }

    private Map<Long, Double> averageFps(List<Long> sessionIds) {
        Map<Long, Double> result = new HashMap<>();
        if (sessionIds.isEmpty()) {
            return result;
        }
        jdbc.query("select session_id, round(avg(fps), 2) fps from metric_sample where session_id in (:ids) group by session_id",
                Map.of("ids", sessionIds), rs -> {
                    double fps = rs.getDouble("fps");
                    result.put(rs.getLong("session_id"), rs.wasNull() ? null : fps);
                });
        return result;
    }
}
