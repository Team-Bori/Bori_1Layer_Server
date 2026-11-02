package com.example.bori.domain.session.relay;

import com.example.bori.domain.relay.handler.InvalidRelayPayloadException;
import com.example.bori.domain.relay.handler.RelayMessageHandler;
import com.example.bori.domain.relay.handler.RelayPayloadReader;
import com.example.bori.domain.relay.message.RelayEnvelope;
import com.example.bori.domain.relay.message.RelayErrorCode;
import com.example.bori.domain.relay.message.RelayMessageType;
import com.example.bori.domain.relay.socket.RelayConnection;
import com.example.bori.domain.session.deploy.DeployService;
import com.example.bori.domain.session.runtime.DeployStep;
import com.example.bori.domain.session.runtime.SessionRuntime;
import com.example.bori.domain.session.runtime.SessionRuntimeRegistry;
import com.example.bori.domain.session.runtime.StepState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeployProgressHandler implements RelayMessageHandler {

    private final RelayPayloadReader reader;
    private final SessionRuntimeRegistry runtimeRegistry;
    private final DeployService deployService;

    @Override
    public String type() {
        return RelayMessageType.DEPLOY_PROGRESS;
    }

    @Override
    public void handle(RelayConnection connection, RelayEnvelope envelope) {
        SessionRelayPayloads.DeployProgress p = reader.read(envelope, SessionRelayPayloads.DeployProgress.class);
        DeployStep step = DeployStep.fromWire(p.step());
        StepState state = StepState.fromWire(p.state());
        if (p.sessionId() == null || step == null || step == DeployStep.FETCH || state == null) {
            throw new InvalidRelayPayloadException("sessionId, step(transfer|install|run), state(started|done|failed) are required");
        }
        SessionRuntime runtime = runtimeRegistry.get(p.sessionId())
                .filter(rt -> rt.getPiId().equals(connection.getPiId()))
                .orElseThrow(() -> new InvalidRelayPayloadException(RelayErrorCode.SESSION_NOT_FOUND, String.valueOf(p.sessionId())));
        deployService.onProgress(runtime, step, state, p.message());
    }
}
