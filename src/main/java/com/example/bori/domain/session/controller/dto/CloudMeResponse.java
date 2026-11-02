package com.example.bori.domain.session.controller.dto;

import com.example.bori.domain.queue.controller.dto.MyQueueResponse;

// 헤더·화면 복원용: 내 진행 중 세션과 대기 (없으면 null)
public record CloudMeResponse(
        MySessionResponse session,
        MyQueueResponse queue
) {
}
