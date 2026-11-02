package com.example.bori.domain.relay.socket;

import com.example.bori.domain.relay.message.RelayEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RelaySender {

    private final RelayConnectionRegistry registry;
    private final ObjectMapper objectMapper;

    // 연결이 없거나 전송에 실패하면 false
    public boolean send(Long piId, String type, Object data) {
        return registry.find(piId)
                .map(connection -> send(connection, type, null, data))
                .orElse(false);
    }

    public boolean send(RelayConnection connection, String type, String replyTo, Object data) {
        RelayEnvelope envelope = RelayEnvelope.of(type, replyTo, objectMapper.valueToTree(data));
        return connection.sendText(objectMapper.writeValueAsString(envelope));
    }
}
