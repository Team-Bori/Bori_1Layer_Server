package com.example.bori.global.config;

import com.example.bori.domain.relay.socket.RelayHandshakeInterceptor;
import com.example.bori.domain.relay.socket.RelayWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final RelayWebSocketHandler relayWebSocketHandler;
    private final RelayHandshakeInterceptor relayHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // L2는 브라우저가 아니라 Origin 검사를 하지 않는다. 인증은 토큰으로 한다.
        registry.addHandler(relayWebSocketHandler, "/ws/l2")
                .addInterceptors(relayHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }

    // 기본 버퍼(8 KiB)로는 밀린 metric 묶음(최대 60개)이나 파일 조각을 받을 수 없다.
    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(256 * 1024);
        container.setMaxBinaryMessageBufferSize(512 * 1024);
        return container;
    }
}
