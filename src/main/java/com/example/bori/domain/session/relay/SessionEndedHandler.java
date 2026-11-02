package com.example.bori.domain.session.relay;

import com.example.bori.domain.relay.handler.InvalidRelayPayloadException;
import com.example.bori.domain.relay.handler.RelayMessageHandler;
import com.example.bori.domain.relay.handler.RelayPayloadReader;
import com.example.bori.domain.relay.message.RelayEnvelope;
import com.example.bori.domain.relay.message.RelayMessageType;
import com.example.bori.domain.relay.socket.RelayConnection;
import com.example.bori.domain.session.domain.BoardSessionRepository;
import com.example.bori.domain.session.domain.EndReason;
import com.example.bori.domain.session.service.SessionLifecycleService;
import com.example.bori.domain.stat.service.BoardModelStatService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

// L2가 실행을 끝냈다. stop에 대한 응답이면 DB 세션은 이미 끝나 있고, 요약 지표만 반영한다.
// L2가 스스로 멈췄으면(오류) 여기서 세션을 FAILED로 끝낸다.
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEndedHandler implements RelayMessageHandler {

    private final RelayPayloadReader reader;
    private final BoardSessionRepository boardSessionRepository;
    private final SessionLifecycleService lifecycleService;
    private final BoardModelStatService statService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public String type() {
        return RelayMessageType.SESSION_ENDED;
    }

    @Override
    public void handle(RelayConnection connection, RelayEnvelope envelope) {
        SessionRelayPayloads.SessionEnded ended = reader.read(envelope, SessionRelayPayloads.SessionEnded.class);
        if (ended.sessionId() == null) {
            throw new InvalidRelayPayloadException("sessionId is required");
        }
        Long sessionId = ended.sessionId();
        boolean failed = "failed".equals(ended.reason());
        lifecycleService.end(sessionId, failed ? EndReason.FAILED : reasonOf(ended.reason()),
                failed ? (ended.error() == null ? "보드에서 실행이 멈췄습니다." : ended.error()) : null, false);

        // 검증 모델이고 실패가 아닌 실행만 보드×모델 통계에 넣는다.
        SessionRelayPayloads.Summary summary = ended.summary();
        if (summary == null || summary.avgFps() == null) {
            return;
        }
        Optional<StatTarget> target = Optional.ofNullable(transactionTemplate.execute(status ->
                boardSessionRepository.findWithBoard(sessionId)
                        .filter(bs -> bs.isVerifiedModel() && bs.getEndReason() != EndReason.FAILED)
                        .map(bs -> new StatTarget(bs.getCloudBoard().getId(), bs.getAiModel().getId()))
                        .orElse(null)));
        target.ifPresent(t -> statService.addRun(t.boardId(), t.modelId(), summary.avgFps(), summary.avgAccuracyPct()));
    }

    private record StatTarget(Long boardId, Long modelId) {
    }

    private EndReason reasonOf(String reason) {
        return switch (reason == null ? "" : reason) {
            case "idle" -> EndReason.IDLE;
            case "forced" -> EndReason.FORCED;
            default -> EndReason.NORMAL;
        };
    }
}
