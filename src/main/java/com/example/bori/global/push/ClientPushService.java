package com.example.bori.global.push;

import com.example.bori.global.tx.AfterCommit;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

// 브라우저로 {"type": ..., "data": ...}를 보낸다. 트랜잭션 안에서 부르면 커밋 뒤에 보낸다.
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientPushService {

    private final ClientConnectionRegistry registry;
    private final ObjectMapper objectMapper;

    public void push(Long userId, String type, Object data) {
        AfterCommit.run(() -> send(userId, type, data));
    }

    private void send(Long userId, String type, Object data) {
        var sessions = registry.of(userId);
        if (sessions.isEmpty()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("data", data);
        String json = objectMapper.writeValueAsString(body);
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (IOException | RuntimeException e) {
                log.debug("사용자 {}에게 {} 전송 실패: {}", userId, type, e.getMessage());
            }
        }
    }
}
