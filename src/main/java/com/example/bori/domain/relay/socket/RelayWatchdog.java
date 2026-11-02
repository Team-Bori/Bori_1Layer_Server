package com.example.bori.domain.relay.socket;

import com.example.bori.domain.relay.service.RelayBoardService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RelayWatchdog {

    private static final Duration HELLO_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration INACTIVE_TIMEOUT = Duration.ofSeconds(30);

    private final RelayConnectionRegistry registry;
    private final RelayBoardService relayBoardService;

    // 서버가 다시 시작되면 모든 보드를 오프라인으로 두고 hello를 기다린다 (§5 끊김)
    @EventListener(ApplicationReadyEvent.class)
    public void resetOnStartup() {
        int count = relayBoardService.markAllOffline();
        log.info("기동: 보드 {}개를 오프라인으로 초기화", count);
    }

    @Scheduled(fixedDelay = 5_000)
    public void check() {
        Instant now = Instant.now();
        for (RelayConnection connection : registry.all()) {
            if (!connection.isHelloReceived() && connection.getConnectedAt().plus(HELLO_TIMEOUT).isBefore(now)) {
                log.info("릴레이 Pi {}: 10초 안에 hello가 없어 닫는다", connection.getPiId());
                connection.close(RelayConnection.HELLO_TIMEOUT);
            } else if (connection.getLastSeenAt().plus(INACTIVE_TIMEOUT).isBefore(now)) {
                log.info("릴레이 Pi {}: 30초 동안 메시지가 없어 닫는다", connection.getPiId());
                connection.close(RelayConnection.INACTIVE);
            }
        }
    }
}
