package com.example.bori.domain.relay.socket;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// 파이 하나당 연결 하나. 같은 파이가 다시 붙으면 이전 연결을 닫는다.
@Component
public class RelayConnectionRegistry {

    private final Map<Long, RelayConnection> connections = new ConcurrentHashMap<>();

    public void register(RelayConnection connection) {
        RelayConnection previous = connections.put(connection.getPiId(), connection);
        if (previous != null && previous != connection) {
            previous.close(RelayConnection.REPLACED);
        }
    }

    // 지금 등록된 연결이 이 연결일 때만 지운다. 교체된 이전 연결이 닫힐 때 새 연결을 지우지 않게.
    public boolean unregister(RelayConnection connection) {
        return connections.remove(connection.getPiId(), connection);
    }

    public Optional<RelayConnection> find(Long piId) {
        return Optional.ofNullable(connections.get(piId)).filter(RelayConnection::isOpen);
    }

    public boolean isConnected(Long piId) {
        return find(piId).isPresent();
    }

    public Collection<RelayConnection> all() {
        return connections.values();
    }

    // 이 시리얼을 보고한 연결 (hello를 마친 연결만)
    public Optional<RelayConnection> findReporting(String serialCode) {
        return connections.values().stream()
                .filter(c -> c.isHelloReceived() && c.isOpen())
                .filter(c -> c.reportedSlot(serialCode).isPresent())
                .findFirst();
    }
}
