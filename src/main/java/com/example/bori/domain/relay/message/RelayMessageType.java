package com.example.bori.domain.relay.message;

public final class RelayMessageType {

    // L2 → L1
    public static final String HELLO = "hello";
    public static final String PING = "ping";
    public static final String BOARD_ATTACHED = "board_attached";
    public static final String BOARD_DETACHED = "board_detached";
    public static final String FILE_ACK = "file_ack";
    public static final String DEPLOY_PROGRESS = "deploy_progress";
    public static final String METRIC = "metric";
    public static final String SERIAL_LOG = "serial_log";
    public static final String SESSION_ENDED = "session_ended";
    public static final String ERROR = "error";

    // L1 → L2
    public static final String HELLO_ACK = "hello_ack";
    public static final String PONG = "pong";
    public static final String DEPLOY = "deploy";
    public static final String FILE_BEGIN = "file_begin";
    public static final String FILE_END = "file_end";
    public static final String STOP = "stop";

    private RelayMessageType() {
    }
}
