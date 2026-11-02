package com.example.bori.domain.session.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

// 진행 중 세션의 메모리 상태 (배포 단계, 전송 중인 파일). DB에는 남기지 않는다.
// 서버가 재시작되면 사라지고, L2 hello의 runningSessions로 실행 중 세션만 되살린다.
@Getter
public class SessionRuntime {

    private final Long sessionId;
    private final Long userId;
    private final Long piId;
    private final Long boardId;
    private final String serialCode;
    private final Map<DeployStep, StepState> steps = new EnumMap<>(DeployStep.class);
    private final Map<DeployStep, String> stepMessages = new EnumMap<>(DeployStep.class);

    @Setter
    private volatile SessionPhase phase;
    @Setter
    private volatile UUID transferId;
    @Setter
    private volatile String deployMessageId;
    @Setter
    private volatile boolean deploySent;
    @Setter
    private volatile Instant deadline;
    private volatile Path file;
    private volatile String sha256;
    private volatile long sizeBytes;
    private volatile int fileAttempts;

    public SessionRuntime(Long sessionId, Long userId, Long piId, Long boardId, String serialCode, SessionPhase phase) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.piId = piId;
        this.boardId = boardId;
        this.serialCode = serialCode;
        this.phase = phase;
        // 실행 중으로 만들어지는 런타임은 L2 보고로 이어받은 세션이다 → L2에 이미 배포돼 있다.
        this.deploySent = phase == SessionPhase.RUNNING;
        StepState initial = phase == SessionPhase.RUNNING ? StepState.DONE : StepState.WAITING;
        for (DeployStep step : DeployStep.values()) {
            steps.put(step, initial);
        }
    }

    public synchronized void updateStep(DeployStep step, StepState state, String message) {
        steps.put(step, state);
        if (message != null) {
            stepMessages.put(step, message);
        }
    }

    public synchronized List<StepView> stepViews() {
        List<StepView> views = new ArrayList<>();
        for (DeployStep step : DeployStep.values()) {
            views.add(new StepView(step.getWire(), steps.get(step), stepMessages.get(step)));
        }
        return views;
    }

    public void attachFile(Path file, String sha256, long sizeBytes) {
        this.file = file;
        this.sha256 = sha256;
        this.sizeBytes = sizeBytes;
    }

    public synchronized int incrementFileAttempts() {
        return ++fileAttempts;
    }

    public void deleteFile() {
        Path f = file;
        file = null;
        if (f != null) {
            try {
                Files.deleteIfExists(f);
            } catch (IOException ignored) {
                // 임시 파일이라 지우지 못해도 다음 정리에서 사라진다
            }
        }
    }

    public record StepView(String step, StepState state, String message) {
    }
}
