package com.example.bori.domain.relay.handler;

import com.example.bori.domain.relay.event.RelayEvents;
import com.example.bori.domain.relay.message.RelayEnvelope;
import com.example.bori.domain.relay.message.RelayMessageType;
import com.example.bori.domain.relay.message.RelayPayloads;
import com.example.bori.domain.relay.service.RelayBoardService;
import com.example.bori.domain.relay.socket.RelayConnection;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardDetachedHandler implements RelayMessageHandler {

    private final RelayBoardService relayBoardService;
    private final RelayPayloadReader reader;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String type() {
        return RelayMessageType.BOARD_DETACHED;
    }

    @Override
    public void handle(RelayConnection connection, RelayEnvelope envelope) {
        RelayPayloads.BoardDetached detached = reader.read(envelope, RelayPayloads.BoardDetached.class);
        if (detached.serialCode() == null || detached.serialCode().isBlank()) {
            throw new InvalidRelayPayloadException("serialCode is required");
        }
        connection.reportDetached(detached.serialCode());
        relayBoardService.detach(connection.getPiId(), detached.serialCode())
                .ifPresent(boardId -> eventPublisher.publishEvent(new RelayEvents.BoardDetached(boardId, detached.reason())));
    }
}
