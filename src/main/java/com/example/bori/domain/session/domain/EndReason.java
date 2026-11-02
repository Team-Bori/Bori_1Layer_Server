package com.example.bori.domain.session.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EndReason {
    NORMAL("정상 종료"),
    IDLE("유휴 자동 종료"),
    FAILED("실행 실패"),
    FORCED("강제 종료");

    private final String label;
}
