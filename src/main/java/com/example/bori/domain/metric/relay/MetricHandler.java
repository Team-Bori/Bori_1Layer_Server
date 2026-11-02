package com.example.bori.domain.metric.relay;

import com.example.bori.domain.metric.service.MetricWriter;
import com.example.bori.domain.relay.handler.InvalidRelayPayloadException;
import com.example.bori.domain.relay.handler.RelayMessageHandler;
import com.example.bori.domain.relay.handler.RelayPayloadReader;
import com.example.bori.domain.relay.message.RelayEnvelope;
import com.example.bori.domain.relay.message.RelayMessageType;
import com.example.bori.domain.relay.socket.RelayConnection;
import com.example.bori.domain.session.runtime.SessionRuntimeRegistry;
import com.example.bori.global.push.ClientPushService;
import com.example.bori.global.push.PushType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 저장하고, 그 세션 학생 브라우저에 그대로 보낸다. 끝난 세션의 늦은 지표는 버린다.
@Component
@RequiredArgsConstructor
public class MetricHandler implements RelayMessageHandler {

    private static final int MAX_SAMPLES = 60;

    private final RelayPayloadReader reader;
    private final SessionRuntimeRegistry runtimeRegistry;
    private final MetricWriter metricWriter;
    private final ClientPushService clientPushService;

    @Override
    public String type() {
        return RelayMessageType.METRIC;
    }

    @Override
    public void handle(RelayConnection connection, RelayEnvelope envelope) {
        MetricPayloads.Metric metric = reader.read(envelope, MetricPayloads.Metric.class);
        if (metric.sessionId() == null || metric.samples() == null) {
            throw new InvalidRelayPayloadException("sessionId and samples are required");
        }
        List<MetricPayloads.Sample> samples = metric.samples().stream().filter(Objects::nonNull).toList();
        if (samples.isEmpty()) {
            return;
        }
        if (samples.size() > MAX_SAMPLES) {
            throw new InvalidRelayPayloadException("samples must be at most " + MAX_SAMPLES);
        }
        runtimeRegistry.get(metric.sessionId())
                .filter(rt -> rt.getPiId().equals(connection.getPiId()))
                .ifPresent(rt -> {
                    metricWriter.write(rt.getSessionId(), samples);
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("sessionId", rt.getSessionId());
                    data.put("samples", samples);
                    clientPushService.push(rt.getUserId(), PushType.METRIC, data);
                });
    }
}
