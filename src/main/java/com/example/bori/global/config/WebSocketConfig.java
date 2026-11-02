package com.example.bori.global.config;

import com.example.bori.domain.relay.socket.RelayHandshakeInterceptor;
import com.example.bori.domain.relay.socket.RelayWebSocketHandler;
import com.example.bori.global.push.ClientWebSocketHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final RelayWebSocketHandler relayWebSocketHandler;
    private final RelayHandshakeInterceptor relayHandshakeInterceptor;
    private final ClientWebSocketHandler clientWebSocketHandler;

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // L2는 브라우저가 아니라 Origin 검사를 하지 않는다. 인증은 토큰으로 한다.
        registry.addHandler(relayWebSocketHandler, "/ws/l2")
                .addInterceptors(relayHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
        // 브라우저 채널은 쿠키 인증이라 허용 출처를 CORS와 같게 제한한다.
        registry.addHandler(clientWebSocketHandler, "/ws/client")
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }
}
