package com.example.bori.domain.metric.relay;

import com.example.bori.domain.relay.handler.InvalidRelayPayloadException;
import com.example.bori.domain.relay.handler.RelayMessageHandler;
import com.example.bori.domain.relay.handler.RelayPayloadReader;
import com.example.bori.domain.relay.message.RelayEnvelope;
import com.example.bori.domain.relay.message.RelayMessageType;
import com.example.bori.domain.relay.socket.RelayConnection;
import com.example.bori.domain.session.runtime.SessionRuntimeRegistry;
import com.example.bori.global.push.ClientPushService;
import com.example.bori.global.push.PushType;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// MCU 시리얼 출력. 실시간 전달만 하고 저장하지 않는다.
@Component
@RequiredArgsConstructor
public class SerialLogHandler implements RelayMessageHandler {

    private final RelayPayloadReader reader;
    private final SessionRuntimeRegistry runtimeRegistry;
    private final ClientPushService clientPushService;

    @Override
    public String type() {
        return RelayMessageType.SERIAL_LOG;
    }

    @Override
    public void handle(RelayConnection connection, RelayEnvelope envelope) {
        MetricPayloads.SerialLog log = reader.read(envelope, MetricPayloads.SerialLog.class);
        if (log.sessionId() == null || log.lines() == null) {
            throw new InvalidRelayPayloadException("sessionId and lines are required");
        }
        runtimeRegistry.get(log.sessionId())
                .filter(rt -> rt.getPiId().equals(connection.getPiId()))
                .ifPresent(rt -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("sessionId", rt.getSessionId());
                    data.put("lines", log.lines());
                    clientPushService.push(rt.getUserId(), PushType.SERIAL_LOG, data);
                });
    }
}
