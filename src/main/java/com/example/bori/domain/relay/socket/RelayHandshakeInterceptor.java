package com.example.bori.domain.relay.socket;

import com.example.bori.domain.pi.domain.HostPiRepository;
import com.example.bori.domain.pi.service.PiTokenGenerator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

// Authorization: Bearer <파이 토큰>. 틀리면 401로 핸드셰이크를 거절한다.
@Component
@RequiredArgsConstructor
public class RelayHandshakeInterceptor implements HandshakeInterceptor {

    public static final String PI_ID = "piId";
    private static final String BEARER = "Bearer ";

    private final HostPiRepository hostPiRepository;
    private final PiTokenGenerator tokenGenerator;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER) || header.length() == BEARER.length()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        String tokenHash = tokenGenerator.hash(header.substring(BEARER.length()).trim());
        return hostPiRepository.findByTokenHash(tokenHash)
                .map(pi -> {
                    attributes.put(PI_ID, pi.getId());
                    return true;
                })
                .orElseGet(() -> {
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                });
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
