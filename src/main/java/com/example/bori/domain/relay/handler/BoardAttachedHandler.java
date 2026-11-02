package com.example.bori.domain.relay.handler;

import com.example.bori.domain.relay.message.RelayEnvelope;
import com.example.bori.domain.relay.message.RelayMessageType;
import com.example.bori.domain.relay.message.RelayPayloads;
import com.example.bori.domain.relay.service.RelayBoardService;
import com.example.bori.domain.relay.socket.RelayConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardAttachedHandler implements RelayMessageHandler {

    private final RelayBoardService relayBoardService;
    private final RelayPayloadReader reader;

    @Override
    public String type() {
        return RelayMessageType.BOARD_ATTACHED;
    }

    @Override
    public void handle(RelayConnection connection, RelayEnvelope envelope) {
        RelayPayloads.BoardReport report = reader.read(envelope, RelayPayloads.BoardReport.class);
        if (report.serialCode() == null || report.serialCode().isBlank()) {
            throw new InvalidRelayPayloadException("serialCode is required");
        }
        connection.reportAttached(report.serialCode(), report.usbSlot());
        if (!relayBoardService.attach(connection.getPiId(), report.serialCode(), report.usbSlot())) {
            log.info("릴레이 Pi {}: 미등록 보드 {} 연결됨 (무시)", connection.getPiId(), report.serialCode());
        }
    }
}
