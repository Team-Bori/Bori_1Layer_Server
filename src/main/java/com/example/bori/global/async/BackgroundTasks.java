package com.example.bori.global.async;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 배포(다운로드·파일 전송)와 커밋 뒤 후속 작업을 요청 스레드 밖에서 돌린다.
// Executor 빈으로 노출하면 Boot의 기본 applicationTaskExecutor 자동 설정이 꺼져서 일부러 감쌌다.
@Slf4j
@Component
public class BackgroundTasks {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public void run(String name, Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                log.error("백그라운드 작업 실패: {}", name, e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
