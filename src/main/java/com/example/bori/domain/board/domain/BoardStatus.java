package com.example.bori.domain.board.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 관리 상태만 둔다. "사용중"은 진행 중 세션이 있는지로 계산한다.
@Getter
@RequiredArgsConstructor
public enum BoardStatus {
    NORMAL("정상"),
    MAINTENANCE("점검중"),
    BROKEN("고장");

    private final String label;
}
