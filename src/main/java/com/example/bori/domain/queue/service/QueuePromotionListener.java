package com.example.bori.domain.queue.service;

import com.example.bori.domain.queue.event.BoardFreedEvent;
import com.example.bori.global.async.BackgroundTasks;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// QueueService 안에서 this.promoteNext()를 부르면 트랜잭션 프록시를 건너뛴다. 그래서 별도 빈에서 부른다.
// afterCommit 안에서 바로 DB를 쓰면 끝난 트랜잭션에 참여하므로 다른 스레드에서 새 트랜잭션으로 돌린다.
@Component
@RequiredArgsConstructor
public class QueuePromotionListener {

    private final QueueService queueService;
    private final BackgroundTasks backgroundTasks;

    @TransactionalEventListener(fallbackExecution = true)
    public void onBoardFreed(BoardFreedEvent event) {
        backgroundTasks.run("promote queue " + event.boardId(), () -> queueService.promoteNext(event.boardId()));
    }
}
