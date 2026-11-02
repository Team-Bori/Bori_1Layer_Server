package com.example.bori.domain.session.relay;

import com.example.bori.domain.relay.handler.InvalidRelayPayloadException;
import com.example.bori.domain.relay.handler.RelayMessageHandler;
import com.example.bori.domain.relay.handler.RelayPayloadReader;
import com.example.bori.domain.relay.message.RelayEnvelope;
import com.example.bori.domain.relay.message.RelayMessageType;
import com.example.bori.domain.relay.socket.RelayConnection;
import com.example.bori.domain.session.deploy.DeployService;
import com.example.bori.domain.session.runtime.SessionRuntimeRegistry;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileAckHandler implements RelayMessageHandler {

    private final RelayPayloadReader reader;
    private final SessionRuntimeRegistry runtimeRegistry;
    private final DeployService deployService;

    @Override
    public String type() {
        return RelayMessageType.FILE_ACK;
    }

    @Override
    public void handle(RelayConnection connection, RelayEnvelope envelope) {
        SessionRelayPayloads.FileAck ack = reader.read(envelope, SessionRelayPayloads.FileAck.class);
        UUID transferId;
        try {
            transferId = UUID.fromString(ack.transferId());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidRelayPayloadException("transferId is invalid");
        }
        runtimeRegistry.findByTransferId(transferId)
                .filter(rt -> rt.getPiId().equals(connection.getPiId()))
                .ifPresent(rt -> deployService.onFileAck(rt, Boolean.TRUE.equals(ack.ok()), ack.error()));
    }
}
