package com.example.bori.domain.board.domain;

// 학생 화면 배지. 위에서부터 먼저 맞는 것 하나.
public enum CloudAvailability {
    OFFLINE,
    MAINTENANCE,
    BROKEN,
    NOT_EXECUTABLE,
    MY_SESSION,
    MY_TURN,
    BUSY,
    RESERVED,
    FREE
}
