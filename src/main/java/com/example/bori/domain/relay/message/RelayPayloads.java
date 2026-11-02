package com.example.bori.domain.relay.message;

import java.util.List;

// 메시지별 data 모양. 필드 이름은 규격 문서와 같다.
public final class RelayPayloads {

    public record Hello(String agentVersion, String piSerial, List<BoardReport> boards, List<RunningSession> runningSessions) {
    }

    public record BoardReport(String serialCode, String usbSlot) {
    }

    public record RunningSession(Long sessionId, String serialCode) {
    }

    public record HelloAck(Long piId, int pingIntervalSec, int metricIntervalMs, List<String> unknownBoards) {
    }

    public record BoardDetached(String serialCode, String reason) {
    }

    public record Error(String code, String message) {
    }

    private RelayPayloads() {
    }
}
