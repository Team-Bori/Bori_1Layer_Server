package com.example.bori.domain.relay.message;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

// L1↔L2 텍스트 프레임 봉투 (docs/protocol/l1-l2-websocket.md §2)
public record RelayEnvelope(
        String type,
        String id,
        String replyTo,
        Instant ts,
        JsonNode data
) {

    public static RelayEnvelope of(String type, String replyTo, JsonNode data) {
        return new RelayEnvelope(type, UUID.randomUUID().toString(), replyTo, Instant.now(), data);
    }
}
