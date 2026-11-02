package com.example.bori.domain.relay.socket;

import com.example.bori.domain.relay.message.RelayEnvelope;
import java.util.Optional;
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
        return sendEnvelope(connection, RelayEnvelope.of(type, replyTo, objectMapper.valueToTree(data)));
    }

    // 응답(error 등)을 replyTo로 짝지어야 하는 명령. 보냈으면 메시지 id를 돌려준다.
    public Optional<String> sendRequest(Long piId, String type, Object data) {
        RelayEnvelope envelope = RelayEnvelope.of(type, null, objectMapper.valueToTree(data));
        return registry.find(piId)
                .filter(connection -> sendEnvelope(connection, envelope))
                .map(connection -> envelope.id());
    }

    public boolean sendBinary(Long piId, byte[] frame) {
        return registry.find(piId).map(connection -> connection.sendBinary(frame)).orElse(false);
    }

    private boolean sendEnvelope(RelayConnection connection, RelayEnvelope envelope) {
        return connection.sendText(objectMapper.writeValueAsString(envelope));
    }
}
