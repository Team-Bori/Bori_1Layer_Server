package com.example.bori.domain.metric.controller.dto;

import com.example.bori.domain.session.domain.EndReason;
import java.time.LocalDateTime;

// 내 사용량의 세션 한 줄. 진행 중이면 endedAt·endReason이 null이고 durationSec은 지금까지.
public record SessionHistoryResponse(
        Long id,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long boardId,
        String boardName,
        boolean boardDeleted,
        String modelLabel,
        long durationSec,
        Double avgFps,
        EndReason endReason
) {
}
