package com.example.bori.domain.relay.handler;

import com.example.bori.domain.relay.event.RelayEvents;
import com.example.bori.domain.relay.message.RelayEnvelope;
import com.example.bori.domain.relay.message.RelayErrorCode;
import com.example.bori.domain.relay.message.RelayMessageType;
import com.example.bori.domain.relay.message.RelayPayloads;
import com.example.bori.domain.relay.service.RelayBoardService;
import com.example.bori.domain.relay.service.RelayBoardService.HelloResult;
import com.example.bori.domain.relay.socket.RelayConnection;
import com.example.bori.domain.relay.socket.RelaySender;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HelloHandler implements RelayMessageHandler {

    public static final int PING_INTERVAL_SEC = 10;
    public static final int METRIC_INTERVAL_MS = 1000;

    private final RelayBoardService relayBoardService;
    private final RelaySender relaySender;
    private final RelayPayloadReader reader;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String type() {
        return RelayMessageType.HELLO;
    }

    @Override
    public void handle(RelayConnection connection, RelayEnvelope envelope) {
        RelayPayloads.Hello hello = reader.read(envelope, RelayPayloads.Hello.class);
        if (hello.piSerial() == null) {
            throw new InvalidRelayPayloadException("piSerial is required");
        }
        HelloResult result = relayBoardService.handleHello(connection.getPiId(), hello.piSerial(), hello.boards());
        if (result.piMismatch()) {
            log.warn("릴레이 Pi {}: 토큰과 시리얼({})이 다르다", connection.getPiId(), hello.piSerial());
            relaySender.send(connection, RelayMessageType.ERROR, envelope.id(),
                    new RelayPayloads.Error(RelayErrorCode.PI_MISMATCH.name(), "piSerial does not match token"));
            connection.close(RelayConnection.PI_MISMATCH);
            return;
        }
        connection.replaceReportedBoards(result.reported());
        connection.markHelloReceived();
        relaySender.send(connection, RelayMessageType.HELLO_ACK, envelope.id(), new RelayPayloads.HelloAck(
                connection.getPiId(), PING_INTERVAL_SEC, METRIC_INTERVAL_MS, result.unknownBoards()));
        log.info("릴레이 Pi {} hello: 보드 {}개, 미등록 {}개 (agent {})",
                connection.getPiId(), result.reported().size(), result.unknownBoards().size(), hello.agentVersion());
        eventPublisher.publishEvent(new RelayEvents.HelloAccepted(connection.getPiId(),
                hello.runningSessions() == null ? List.of() : hello.runningSessions()));
    }
}
