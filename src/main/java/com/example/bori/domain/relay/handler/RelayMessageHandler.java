package com.example.bori.domain.relay.handler;

import com.example.bori.domain.relay.message.RelayEnvelope;
import com.example.bori.domain.relay.socket.RelayConnection;

// L2 → L1 메시지 종류 하나를 처리한다. 빈으로 등록하면 디스패처가 type으로 찾는다.
public interface RelayMessageHandler {

    String type();

    void handle(RelayConnection connection, RelayEnvelope envelope);
}
