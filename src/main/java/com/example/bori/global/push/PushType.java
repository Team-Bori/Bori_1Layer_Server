package com.example.bori.global.push;

// 브라우저로 보내는 메시지 종류
public final class PushType {

    public static final String DEPLOY_PROGRESS = "deploy_progress";
    public static final String SESSION_STARTED = "session_started";
    public static final String SESSION_ENDED = "session_ended";
    public static final String METRIC = "metric";
    public static final String SERIAL_LOG = "serial_log";
    public static final String QUEUE = "queue";
    public static final String NOTIFICATION = "notification";

    private PushType() {
    }
}
