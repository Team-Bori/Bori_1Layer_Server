package com.example.bori.domain.relay.socket;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

// 릴레이 Pi 하나와의 WebSocket 연결
@Slf4j
public class RelayConnection {

    public static final CloseStatus REPLACED = new CloseStatus(4001, "replaced");
    public static final CloseStatus HELLO_TIMEOUT = new CloseStatus(4002, "hello timeout");
    public static final CloseStatus INACTIVE = new CloseStatus(4003, "no message for 30s");
    public static final CloseStatus PI_MISMATCH = new CloseStatus(4004, "pi mismatch");

    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT = 8 * 1024 * 1024;

    @Getter
    private final Long piId;
    private final WebSocketSession session;
    @Getter
    private final Instant connectedAt = Instant.now();
    @Getter
    private volatile Instant lastSeenAt = Instant.now();
    @Getter
    private volatile boolean helloReceived;

    // L2가 지금 붙어 있다고 보고한 보드: serialCode → usbSlot (미등록 보드 포함)
    private final Map<String, String> reportedBoards = new ConcurrentHashMap<>();

    public RelayConnection(Long piId, WebSocketSession session) {
        this.piId = piId;
        this.session = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
    }

    public String sessionId() {
        return session.getId();
    }

    public void touch() {
        lastSeenAt = Instant.now();
    }

    public void markHelloReceived() {
        helloReceived = true;
    }

    public void replaceReportedBoards(Map<String, String> boards) {
        reportedBoards.clear();
        reportedBoards.putAll(boards);
    }

    public void reportAttached(String serialCode, String usbSlot) {
        reportedBoards.put(serialCode, usbSlot == null ? "" : usbSlot);
    }

    public void reportDetached(String serialCode) {
        reportedBoards.remove(serialCode);
    }

    public Optional<String> reportedSlot(String serialCode) {
        return Optional.ofNullable(reportedBoards.get(serialCode));
    }

    public boolean isOpen() {
        return session.isOpen();
    }

    public boolean sendText(String payload) {
        try {
            session.sendMessage(new TextMessage(payload));
            return true;
        } catch (IOException | RuntimeException e) {
            log.warn("릴레이 Pi {}에 메시지 전송 실패: {}", piId, e.getMessage());
            return false;
        }
    }

    public boolean sendBinary(byte[] payload) {
        try {
            session.sendMessage(new BinaryMessage(payload));
            return true;
        } catch (IOException | RuntimeException e) {
            log.warn("릴레이 Pi {}에 바이너리 전송 실패: {}", piId, e.getMessage());
            return false;
        }
    }

    public void close(CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException e) {
            log.debug("릴레이 Pi {} 연결 닫기 실패: {}", piId, e.getMessage());
        }
    }
}
