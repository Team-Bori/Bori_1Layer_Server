package com.example.bori.domain.session.relay;

import com.example.bori.domain.board.domain.CloudBoard;
import com.example.bori.domain.relay.event.RelayEvents;
import com.example.bori.domain.relay.message.RelayMessageType;
import com.example.bori.domain.relay.message.RelayPayloads.RunningSession;
import com.example.bori.domain.relay.socket.RelaySender;
import com.example.bori.domain.session.deploy.DeployService;
import com.example.bori.domain.session.domain.BoardSession;
import com.example.bori.domain.session.domain.BoardSessionRepository;
import com.example.bori.domain.session.domain.EndReason;
import com.example.bori.domain.session.runtime.DeployStep;
import com.example.bori.domain.session.runtime.SessionPhase;
import com.example.bori.domain.session.runtime.SessionRuntime;
import com.example.bori.domain.session.runtime.SessionRuntimeRegistry;
import com.example.bori.domain.session.service.SessionLifecycleService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

// L2 연결 상태 변화에 따라 세션을 맞춘다 (규격 §4 hello, §5 끊김).
@Slf4j
@Component
@RequiredArgsConstructor
public class RelaySessionListener {

    private final BoardSessionRepository boardSessionRepository;
    private final SessionRuntimeRegistry runtimeRegistry;
    private final SessionLifecycleService lifecycleService;
    private final DeployService deployService;
    private final RelaySender relaySender;
    private final TransactionTemplate transactionTemplate;

    // 파이가 끊긴 시각. 60초 안에 다시 hello가 오면 진행 중 세션을 이어간다.
    private final Map<Long, Instant> disconnectedAt = new ConcurrentHashMap<>();
    private final Instant startedAt = Instant.now();

    public Instant disconnectedSince(Long piId) {
        return disconnectedAt.getOrDefault(piId, startedAt);
    }

    @EventListener
    public void onDisconnected(RelayEvents.Disconnected event) {
        disconnectedAt.put(event.piId(), Instant.now());
    }

    @EventListener
    public void onHello(RelayEvents.HelloAccepted event) {
        disconnectedAt.remove(event.piId());
        Set<Long> reported = event.runningSessions().stream()
                .map(RunningSession::sessionId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        List<Long> toFail = new ArrayList<>();
        Set<Long> activeIds = transactionTemplate.execute(status -> {
            List<BoardSession> active = boardSessionRepository.findActiveByPiId(event.piId());
            for (BoardSession s : active) {
                Optional<SessionRuntime> runtime = runtimeRegistry.get(s.getId());
                if (reported.contains(s.getId())) {
                    // 서버 재시작 등으로 런타임이 없으면 실행 중으로 이어받는다.
                    CloudBoard board = s.getCloudBoard();
                    runtimeRegistry.putIfAbsent(new SessionRuntime(s.getId(), s.getUser().getId(), event.piId(),
                            board.getId(), board.getSerialCode(), SessionPhase.RUNNING));
                } else if (runtime.isEmpty() || runtime.get().isDeploySent()) {
                    // 아직 GitHub에서 받는 중(L2에 안 보냄)인 세션은 건드리지 않는다.
                    toFail.add(s.getId());
                }
            }
            return active.stream().map(BoardSession::getId).collect(Collectors.toSet());
        });
        toFail.forEach(id -> lifecycleService.end(id, EndReason.FAILED, "릴레이 Pi가 다시 연결됐지만 실행 중이 아닙니다.", false));

        // L2는 돌리고 있는데 L1에선 끝났거나 모르는 세션 → 멈추게 한다.
        reported.stream()
                .filter(id -> activeIds == null || !activeIds.contains(id))
                .forEach(id -> relaySender.send(event.piId(), RelayMessageType.STOP, Map.of("sessionId", id, "reason", "normal")));
    }

    // 실행 중인 보드가 빠지면 세션은 실패로 끝난다.
    @EventListener
    public void onBoardDetached(RelayEvents.BoardDetached event) {
        boardSessionRepository.findByCloudBoardIdAndEndedAtIsNull(event.boardId())
                .ifPresent(s -> lifecycleService.end(s.getId(), EndReason.FAILED,
                        "보드 연결이 끊겼습니다" + (event.reason() == null ? "." : " (" + event.reason() + ")."), false));
    }

    // deploy에 대한 error (BOARD_BUSY 등)
    @EventListener
    public void onRelayError(RelayEvents.ErrorReceived event) {
        if (event.replyTo() == null) {
            return;
        }
        runtimeRegistry.findByDeployMessageId(event.replyTo())
                .ifPresent(rt -> deployService.fail(rt, DeployStep.TRANSFER, "릴레이 Pi가 배포를 거절했습니다 (" + event.code() + ")."));
    }
}
