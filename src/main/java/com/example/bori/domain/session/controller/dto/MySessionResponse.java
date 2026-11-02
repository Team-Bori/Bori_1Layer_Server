package com.example.bori.domain.session.controller.dto;

import com.example.bori.domain.board.domain.BoardType;
import com.example.bori.domain.session.runtime.SessionPhase;
import com.example.bori.domain.session.runtime.SessionRuntime.StepView;
import java.time.LocalDateTime;
import java.util.List;

// idle 남은 시간 = lastActiveAt + idleTimeoutSec - 현재 (phase가 RUNNING일 때만 줄어든다)
public record MySessionResponse(
        Long id,
        Long boardId,
        String boardName,
        BoardType boardType,
        String host,
        String piName,
        String usbSlot,
        String modelLabel,
        LocalDateTime startedAt,
        LocalDateTime lastActiveAt,
        long idleTimeoutSec,
        SessionPhase phase,
        List<StepView> steps
) {
}
