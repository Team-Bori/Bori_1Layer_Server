package com.example.bori.domain.queue.controller.dto;

import com.example.bori.domain.queue.domain.QueueState;
import java.time.LocalDateTime;

// position: 이 보드 활성 대기 중 몇 번째인지 (1부터). READY면 접속 가능, expiresAt까지.
public record MyQueueResponse(
        Long entryId,
        Long boardId,
        String boardName,
        QueueState state,
        int position,
        int capacity,
        LocalDateTime expiresAt
) {
}
