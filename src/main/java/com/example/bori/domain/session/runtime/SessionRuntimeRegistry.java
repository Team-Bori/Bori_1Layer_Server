package com.example.bori.domain.session.runtime;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SessionRuntimeRegistry {

    private final Map<Long, SessionRuntime> runtimes = new ConcurrentHashMap<>();

    public void put(SessionRuntime runtime) {
        runtimes.put(runtime.getSessionId(), runtime);
    }

    public SessionRuntime putIfAbsent(SessionRuntime runtime) {
        SessionRuntime existing = runtimes.putIfAbsent(runtime.getSessionId(), runtime);
        return existing != null ? existing : runtime;
    }

    public Optional<SessionRuntime> get(Long sessionId) {
        return Optional.ofNullable(runtimes.get(sessionId));
    }

    public Optional<SessionRuntime> findByTransferId(UUID transferId) {
        return runtimes.values().stream().filter(r -> transferId.equals(r.getTransferId())).findFirst();
    }

    public Optional<SessionRuntime> findByDeployMessageId(String messageId) {
        return runtimes.values().stream().filter(r -> messageId.equals(r.getDeployMessageId())).findFirst();
    }

    public Collection<SessionRuntime> all() {
        return runtimes.values();
    }

    public void remove(Long sessionId) {
        SessionRuntime removed = runtimes.remove(sessionId);
        if (removed != null) {
            removed.deleteFile();
        }
    }
}
