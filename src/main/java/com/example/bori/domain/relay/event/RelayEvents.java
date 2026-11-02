package com.example.bori.domain.relay.event;

import com.example.bori.domain.relay.message.RelayPayloads.RunningSession;
import java.util.List;

// relay 패키지가 세션·대기열을 모르게 이벤트로 알린다.
public final class RelayEvents {

    // hello를 처리한 뒤. runningSessions로 세션 상태를 맞춘다.
    public record HelloAccepted(Long piId, List<RunningSession> runningSessions) {
    }

    // 연결이 끊긴 뒤 (교체된 연결은 제외)
    public record Disconnected(Long piId) {
    }

    // 등록된 보드가 오프라인이 됐을 때
    public record BoardDetached(Long boardId, String reason) {
    }

    // L2가 보낸 error. replyTo로 어떤 명령이 실패했는지 찾는다.
    public record ErrorReceived(Long piId, String replyTo, String code, String message) {
    }

    private RelayEvents() {
    }
}
