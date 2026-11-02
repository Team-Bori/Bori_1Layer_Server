package com.example.bori.domain.session.relay;

final class SessionRelayPayloads {

    record FileAck(String transferId, Boolean ok, String error) {
    }

    record DeployProgress(Long sessionId, String step, String state, String message) {
    }

    record Summary(Long inferenceCount, Double avgFps, Double avgAccuracyPct) {
    }

    record SessionEnded(Long sessionId, String reason, Summary summary, String error) {
    }

    private SessionRelayPayloads() {
    }
}
