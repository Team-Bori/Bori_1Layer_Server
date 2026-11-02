package com.example.bori.domain.relay.handler;

import com.example.bori.domain.relay.message.RelayEnvelope;
import com.example.bori.domain.relay.message.RelayMessageType;
import com.example.bori.domain.relay.socket.RelayConnection;
import com.example.bori.domain.relay.socket.RelaySender;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PingHandler implements RelayMessageHandler {

    private final RelaySender relaySender;

    @Override
    public String type() {
        return RelayMessageType.PING;
    }

    @Override
    public void handle(RelayConnection connection, RelayEnvelope envelope) {
        relaySender.send(connection, RelayMessageType.PONG, envelope.id(), Map.of());
    }
}
