package com.example.bori.domain.relay.handler;

import com.example.bori.domain.relay.event.RelayEvents;
import com.example.bori.domain.relay.message.RelayEnvelope;
import com.example.bori.domain.relay.message.RelayMessageType;
import com.example.bori.domain.relay.message.RelayPayloads;
import com.example.bori.domain.relay.socket.RelayConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// L2가 명령을 처리하지 못했다는 응답
@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorHandler implements RelayMessageHandler {

    private final RelayPayloadReader reader;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String type() {
        return RelayMessageType.ERROR;
    }

    @Override
    public void handle(RelayConnection connection, RelayEnvelope envelope) {
        RelayPayloads.Error error = reader.read(envelope, RelayPayloads.Error.class);
        log.warn("릴레이 Pi {} error (replyTo={}): {} {}", connection.getPiId(), envelope.replyTo(), error.code(), error.message());
        eventPublisher.publishEvent(new RelayEvents.ErrorReceived(
                connection.getPiId(), envelope.replyTo(), error.code(), error.message()));
    }
}
