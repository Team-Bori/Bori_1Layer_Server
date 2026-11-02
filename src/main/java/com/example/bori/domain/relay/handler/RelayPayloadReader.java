package com.example.bori.domain.relay.handler;

import com.example.bori.domain.relay.message.RelayEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RelayPayloadReader {

    private final ObjectMapper objectMapper;

    public <T> T read(RelayEnvelope envelope, Class<T> type) {
        if (envelope.data() == null || envelope.data().isNull()) {
            throw new InvalidRelayPayloadException("data is required");
        }
        try {
            return objectMapper.treeToValue(envelope.data(), type);
        } catch (JacksonException e) {
            throw new InvalidRelayPayloadException("invalid data for " + envelope.type());
        }
    }
}
