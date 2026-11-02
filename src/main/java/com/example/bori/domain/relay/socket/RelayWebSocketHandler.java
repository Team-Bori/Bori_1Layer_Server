package com.example.bori.domain.relay.socket;

import com.example.bori.domain.relay.event.RelayEvents;
import com.example.bori.domain.relay.handler.InvalidRelayPayloadException;
import com.example.bori.domain.relay.handler.RelayMessageHandler;
import com.example.bori.domain.relay.message.RelayEnvelope;
import com.example.bori.domain.relay.message.RelayErrorCode;
import com.example.bori.domain.relay.message.RelayMessageType;
import com.example.bori.domain.relay.message.RelayPayloads;
import com.example.bori.domain.relay.service.RelayBoardService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class RelayWebSocketHandler extends AbstractWebSocketHandler {

    private static final String CONNECTION = "relayConnection";

    private final RelayConnectionRegistry registry;
    private final RelaySender relaySender;
    private final RelayBoardService relayBoardService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, RelayMessageHandler> handlers;

    public RelayWebSocketHandler(RelayConnectionRegistry registry, RelaySender relaySender,
                                 RelayBoardService relayBoardService, ObjectMapper objectMapper,
                                 ApplicationEventPublisher eventPublisher, List<RelayMessageHandler> handlers) {
        this.registry = registry;
        this.relaySender = relaySender;
        this.relayBoardService = relayBoardService;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.handlers = handlers.stream().collect(Collectors.toMap(RelayMessageHandler::type, Function.identity()));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long piId = (Long) session.getAttributes().get(RelayHandshakeInterceptor.PI_ID);
        RelayConnection connection = new RelayConnection(piId, session);
        session.getAttributes().put(CONNECTION, connection);
        registry.register(connection);
        log.info("릴레이 Pi {} 연결됨 ({})", piId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        RelayConnection connection = connection(session);
        connection.touch();

        RelayEnvelope envelope;
        try {
            envelope = objectMapper.readValue(message.getPayload(), RelayEnvelope.class);
        } catch (JacksonException e) {
            sendError(connection, null, RelayErrorCode.INVALID_PAYLOAD, "invalid json envelope");
            return;
        }
        if (envelope.type() == null) {
            sendError(connection, envelope.id(), RelayErrorCode.INVALID_PAYLOAD, "type is required");
            return;
        }
        // hello 전에 온 다른 메시지는 무시한다 (§1)
        if (!connection.isHelloReceived() && !RelayMessageType.HELLO.equals(envelope.type())) {
            return;
        }

        RelayMessageHandler handler = handlers.get(envelope.type());
        if (handler == null) {
            sendError(connection, envelope.id(), RelayErrorCode.UNKNOWN_TYPE, "unknown type: " + envelope.type());
            return;
        }
        try {
            handler.handle(connection, envelope);
        } catch (InvalidRelayPayloadException e) {
            sendError(connection, envelope.id(), e.getCode(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("릴레이 Pi {} {} 처리 실패", connection.getPiId(), envelope.type(), e);
        }
    }

    // 규격상 L2 → L1 바이너리는 없다.
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        connection(session).touch();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        RelayConnection connection = (RelayConnection) session.getAttributes().get(CONNECTION);
        if (connection == null) {
            return;
        }
        log.info("릴레이 Pi {} 연결 끊김 ({}, {})", connection.getPiId(), session.getId(), status);
        if (registry.unregister(connection)) {
            relayBoardService.markPiOffline(connection.getPiId());
            eventPublisher.publishEvent(new RelayEvents.Disconnected(connection.getPiId()));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("릴레이 WebSocket 전송 오류 ({}): {}", session.getId(), exception.getMessage());
    }

    private RelayConnection connection(WebSocketSession session) {
        return (RelayConnection) session.getAttributes().get(CONNECTION);
    }

    private void sendError(RelayConnection connection, String replyTo, RelayErrorCode code, String message) {
        relaySender.send(connection, RelayMessageType.ERROR, replyTo, new RelayPayloads.Error(code.name(), message));
    }
}
