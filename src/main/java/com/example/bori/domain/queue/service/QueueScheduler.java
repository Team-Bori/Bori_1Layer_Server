package com.example.bori.domain.queue.service;

import com.example.bori.domain.queue.domain.QueueEntryRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueScheduler {

    private final QueueEntryRepository queueEntryRepository;
    private final QueueService queueService;

    @Scheduled(fixedDelay = 5_000)
    public void run() {
        queueEntryRepository.findExpiredReadyIds(LocalDateTime.now()).forEach(queueService::expire);
        // 대기자가 있는데 차례가 아무에게도 없는 보드 (점검 끝남, 오프라인 → 온라인 등)
        queueEntryRepository.findBoardIdsWithWaitingButNoReady().forEach(queueService::promoteIfIdle);
    }
}
