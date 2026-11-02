package com.example.bori.domain.rental.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RentalBoardStatus {
    AVAILABLE("대여가능"),
    RENTED("대여중"),
    RETURN_PENDING("반납대기"),
    MAINTENANCE("점검중"),
    BROKEN("고장");

    private final String label;

    // 관리자가 직접 고를 수 있는 상태. 대여중·반납대기는 대여 흐름으로만 바뀐다.
    public boolean isManual() {
        return this == AVAILABLE || this == MAINTENANCE || this == BROKEN;
    }
}
