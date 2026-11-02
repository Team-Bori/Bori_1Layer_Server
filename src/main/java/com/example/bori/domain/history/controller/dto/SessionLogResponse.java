package com.example.bori.domain.history.controller.dto;

import com.example.bori.domain.session.domain.EndReason;
import java.time.LocalDateTime;

// endReason이 null이면 진행 중
public record SessionLogResponse(
        Long id,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String userName,
        String studentNo,
        Long boardId,
        String boardName,
        boolean boardDeleted,
        String modelLabel,
        long durationSec,
        EndReason endReason
) {
}
