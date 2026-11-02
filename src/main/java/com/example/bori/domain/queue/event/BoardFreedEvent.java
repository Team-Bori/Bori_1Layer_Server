package com.example.bori.domain.queue.event;

// 보드가 비었거나 대기가 빠졌을 때. 커밋 뒤 다음 대기자를 올린다.
public record BoardFreedEvent(Long boardId) {
}
