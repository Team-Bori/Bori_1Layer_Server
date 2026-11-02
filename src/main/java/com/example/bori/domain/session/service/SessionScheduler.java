package com.example.bori.domain.session.service;

import com.example.bori.domain.relay.socket.RelayConnectionRegistry;
import com.example.bori.domain.session.deploy.DeployService;
import com.example.bori.domain.session.domain.BoardSession;
import com.example.bori.domain.session.domain.BoardSessionRepository;
import com.example.bori.domain.session.domain.EndReason;
import com.example.bori.domain.session.relay.RelaySessionListener;
import com.example.bori.domain.session.runtime.DeployStep;
import com.example.bori.domain.session.runtime.SessionPhase;
import com.example.bori.domain.session.runtime.SessionRuntime;
import com.example.bori.domain.session.runtime.SessionRuntimeRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionScheduler {

    public static final Duration IDLE_TIMEOUT = Duration.ofMinutes(15);
    private static final Duration RELAY_GRACE = Duration.ofSeconds(60);

    private final BoardSessionRepository boardSessionRepository;
    private final SessionRuntimeRegistry runtimeRegistry;
    private final SessionLifecycleService lifecycleService;
    private final DeployService deployService;
    private final RelayConnectionRegistry relayConnectionRegistry;
    private final RelaySessionListener relaySessionListener;

    @Scheduled(fixedDelay = 5_000)
    public void run() {
        endIdleSessions();
        endSessionsOfLostRelays();
        failTimedOutDeploys();
    }

    // 실행 중(run 완료) 세션만 유휴 판정한다. 배포 중엔 학생이 할 수 있는 조작이 없다.
    private void endIdleSessions() {
        List<Long> idle = boardSessionRepository.findIdleIds(LocalDateTime.now().minus(IDLE_TIMEOUT));
        for (Long id : idle) {
            boolean deploying = runtimeRegistry.get(id).map(rt -> rt.getPhase() != SessionPhase.RUNNING).orElse(false);
            if (!deploying) {
                lifecycleService.end(id, EndReason.IDLE, "15분 동안 조작이 없어 세션을 종료했습니다.", true);
            }
        }
    }

    // 파이가 끊긴 지 60초가 지나도 다시 붙지 않으면 그 파이의 진행 중 세션은 실패
    private void endSessionsOfLostRelays() {
        Instant now = Instant.now();
        for (BoardSession s : boardSessionRepository.findAllByEndedAtIsNull()) {
            Long piId = s.getCloudBoard().getPi().getId();
            if (!relayConnectionRegistry.isConnected(piId)
                    && relaySessionListener.disconnectedSince(piId).plus(RELAY_GRACE).isBefore(now)) {
                lifecycleService.end(s.getId(), EndReason.FAILED, "릴레이 Pi 연결이 끊겼습니다.", false);
            }
        }
    }

    private void failTimedOutDeploys() {
        Instant now = Instant.now();
        for (SessionRuntime rt : List.copyOf(runtimeRegistry.all())) {
            if (rt.getDeadline() != null && rt.getDeadline().isBefore(now)) {
                deployService.fail(rt, DeployStep.RUN, "배포 시간이 초과되었습니다.");
            }
        }
    }
}
