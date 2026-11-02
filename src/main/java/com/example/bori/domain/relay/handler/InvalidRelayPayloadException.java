package com.example.bori.domain.relay.handler;

import com.example.bori.domain.relay.message.RelayErrorCode;
import lombok.Getter;

@Getter
public class InvalidRelayPayloadException extends RuntimeException {

    private final RelayErrorCode code;

    public InvalidRelayPayloadException(String message) {
        this(RelayErrorCode.INVALID_PAYLOAD, message);
    }

    public InvalidRelayPayloadException(RelayErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
