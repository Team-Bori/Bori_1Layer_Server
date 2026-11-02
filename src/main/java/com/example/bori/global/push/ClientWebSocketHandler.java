package com.example.bori.global.push;

import com.example.bori.global.security.CustomUserDetails;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

// 브라우저 실시간 채널. 서버 → 브라우저 단방향이고, 로그인 세션 쿠키로 인증한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientWebSocketHandler extends TextWebSocketHandler {

    private static final String USER_ID = "userId";
    private static final String DECORATED = "decorated";

    private final ClientConnectionRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = userId(session.getPrincipal());
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        WebSocketSession decorated = new ConcurrentWebSocketSessionDecorator(session, 5_000, 1024 * 1024);
        session.getAttributes().put(USER_ID, userId);
        session.getAttributes().put(DECORATED, decorated);
        registry.add(userId, decorated);
    }

    // 브라우저가 보내는 메시지는 쓰지 않는다 (활동 신호 등은 REST로).
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get(USER_ID);
        WebSocketSession decorated = (WebSocketSession) session.getAttributes().get(DECORATED);
        if (userId != null && decorated != null) {
            registry.remove(userId, decorated);
        }
    }

    private Long userId(Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUserId();
        }
        return null;
    }
}
