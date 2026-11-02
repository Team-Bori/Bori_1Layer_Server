package com.example.bori.global.tx;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 커밋된 뒤에만 외부로 내보낸다 (WebSocket 전송 등). 트랜잭션 밖이면 바로 실행한다.
// 주의: afterCommit 안에서 DB를 쓰면 이미 끝난 트랜잭션에 참여한다. DB 작업은 다른 스레드나 REQUIRES_NEW로.
public final class AfterCommit {

    public static void run(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private AfterCommit() {
    }
}
