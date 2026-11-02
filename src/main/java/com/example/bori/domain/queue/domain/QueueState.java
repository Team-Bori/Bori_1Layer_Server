package com.example.bori.domain.queue.domain;

import java.util.List;

// WAITING → READY(차례, 5분 안에 접속) → ENTERED / EXPIRED, 언제든 CANCELLED
public enum QueueState {
    WAITING,
    READY,
    ENTERED,
    EXPIRED,
    CANCELLED;

    public static final List<QueueState> ACTIVE = List.of(WAITING, READY);
}
