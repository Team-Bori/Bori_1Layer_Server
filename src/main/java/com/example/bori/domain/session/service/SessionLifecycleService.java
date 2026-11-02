package com.example.bori.domain.session.service;

import com.example.bori.domain.queue.event.BoardFreedEvent;
import com.example.bori.domain.relay.message.RelayMessageType;
import com.example.bori.domain.relay.socket.RelaySender;
import com.example.bori.domain.session.domain.BoardSession;
import com.example.bori.domain.session.domain.BoardSessionRepository;
import com.example.bori.domain.session.domain.EndReason;
import com.example.bori.domain.session.runtime.SessionRuntime;
import com.example.bori.domain.session.runtime.SessionRuntimeRegistry;
import com.example.bori.global.push.ClientPushService;
import com.example.bori.global.push.PushType;
import com.example.bori.global.tx.AfterCommit;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 세션 종료는 전부 여기로 모은다 (학생 종료, 유휴, 관리자 강제, 배포·실행 실패, 연결 끊김).
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionLifecycleService {

    private final BoardSessionRepository boardSessionRepository;
    private final SessionRuntimeRegistry runtimeRegistry;
    private final RelaySender relaySender;
    private final ClientPushService clientPushService;
    private final ApplicationEventPublisher eventPublisher;

    // stopRelay: L2에 stop을 보낼지. L2가 먼저 끝냈거나(session_ended) 보드가 빠진 경우엔 false.
    // 이미 끝난 세션이면 false를 돌려준다.
    @Transactional
    public boolean end(Long sessionId, EndReason reason, String message, boolean stopRelay) {
        Optional<BoardSession> found = boardSessionRepository.findForUpdate(sessionId);
        if (found.isEmpty() || !found.get().end(LocalDateTime.now(), reason)) {
            return false;
        }
        BoardSession session = found.get();
        Long userId = session.getUser().getId();
        Long boardId = session.getCloudBoard().getId();
        Long piId = session.getCloudBoard().getPi().getId();

        // 런타임이 없으면 재시작 뒤 L2 보고로 이어받은 실행 중 세션이다 → L2에서 돌고 있다.
        Optional<SessionRuntime> runtime = runtimeRegistry.get(sessionId);
        boolean relayHasIt = runtime.map(SessionRuntime::isDeploySent).orElse(true);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("reason", reason);
        data.put("message", message);
        clientPushService.push(userId, PushType.SESSION_ENDED, data);

        AfterCommit.run(() -> {
            runtimeRegistry.remove(sessionId);
            if (stopRelay && relayHasIt) {
                relaySender.send(piId, RelayMessageType.STOP,
                        Map.of("sessionId", sessionId, "reason", reason.name().toLowerCase()));
            }
        });
        eventPublisher.publishEvent(new BoardFreedEvent(boardId));
        log.info("세션 {} 종료: {}{}", sessionId, reason, message == null ? "" : " (" + message + ")");
        return true;
    }

    @Transactional
    public void touch(Long sessionId) {
        boardSessionRepository.findById(sessionId).ifPresent(s -> s.touch(LocalDateTime.now()));
    }
}
