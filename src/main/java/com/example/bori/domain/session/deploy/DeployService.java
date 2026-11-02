package com.example.bori.domain.session.deploy;

import com.example.bori.domain.relay.message.RelayMessageType;
import com.example.bori.domain.relay.socket.RelaySender;
import com.example.bori.domain.session.deploy.ModelArchiveFetcher.FetchedArchive;
import com.example.bori.domain.session.domain.BoardSession;
import com.example.bori.domain.session.domain.BoardSessionRepository;
import com.example.bori.domain.session.domain.EndReason;
import com.example.bori.domain.session.domain.UserModelUploadRepository;
import com.example.bori.domain.session.runtime.DeployStep;
import com.example.bori.domain.session.runtime.SessionPhase;
import com.example.bori.domain.session.runtime.SessionRuntime;
import com.example.bori.domain.session.runtime.SessionRuntimeRegistry;
import com.example.bori.domain.session.runtime.StepState;
import com.example.bori.domain.session.service.SessionLifecycleService;
import com.example.bori.global.async.BackgroundTasks;
import com.example.bori.global.github.GithubRepoUrl;
import com.example.bori.global.push.ClientPushService;
import com.example.bori.global.push.PushType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

// 배포 흐름 (규격 §5): GitHub zip 받기 → deploy → file_begin/조각/file_end → file_ack → deploy_progress…
// 다운로드·전송은 요청 스레드 밖에서 하고, 이후 단계는 L2 메시지가 올 때마다 이어간다.
@Slf4j
@Service
@RequiredArgsConstructor
public class DeployService {

    public static final int CHUNK_SIZE = 256 * 1024;
    public static final Duration FILE_ACK_TIMEOUT = Duration.ofMinutes(5);
    public static final Duration RUN_TIMEOUT = Duration.ofMinutes(10);
    private static final int MAX_FILE_ATTEMPTS = 2;

    private final SessionRuntimeRegistry runtimeRegistry;
    private final BoardSessionRepository boardSessionRepository;
    private final UserModelUploadRepository uploadRepository;
    private final ModelArchiveFetcher archiveFetcher;
    private final RelaySender relaySender;
    private final ClientPushService clientPushService;
    private final SessionLifecycleService lifecycleService;
    private final BackgroundTasks backgroundTasks;
    private final TransactionTemplate transactionTemplate;

    private record DeployTarget(Long sessionId, String serialCode, String boardType, int limitMb, String source,
                                String modelName, String format, String repoUrl, Long uploadId) {
    }

    public void start(Long sessionId) {
        backgroundTasks.run("deploy session " + sessionId, () -> deploy(sessionId));
    }

    private void deploy(Long sessionId) {
        DeployTarget target = transactionTemplate.execute(status -> loadTarget(sessionId));
        Optional<SessionRuntime> found = runtimeRegistry.get(sessionId);
        if (target == null || found.isEmpty()) {
            return;
        }
        SessionRuntime runtime = found.get();
        progress(runtime, DeployStep.FETCH, StepState.STARTED, null);

        FetchedArchive archive;
        try {
            archive = archiveFetcher.fetch(GithubRepoUrl.parse(target.repoUrl()), target.limitMb());
        } catch (DeployFailure e) {
            fail(runtime, DeployStep.FETCH, e.getMessage());
            return;
        }
        runtime.attachFile(archive.path(), archive.sha256(), archive.sizeBytes());
        if (runtimeRegistry.get(sessionId).isEmpty()) {
            // 받는 동안 세션이 끝났다
            runtime.deleteFile();
            return;
        }
        if (target.uploadId() != null) {
            transactionTemplate.executeWithoutResult(status -> uploadRepository.findById(target.uploadId())
                    .ifPresent(upload -> upload.updateActualSize(archive.sizeMbCeil())));
        }
        progress(runtime, DeployStep.FETCH, StepState.DONE, null);
        sendDeploy(runtime, target);
    }

    private DeployTarget loadTarget(Long sessionId) {
        return boardSessionRepository.findWithBoard(sessionId)
                .filter(BoardSession::isActive)
                .map(s -> {
                    var board = s.getCloudBoard();
                    boolean verified = s.isVerifiedModel();
                    return new DeployTarget(s.getId(), board.getSerialCode(), board.getBoardType().name().toLowerCase(),
                            board.getUploadLimitMb(),
                            verified ? "verified" : "upload",
                            verified ? s.getAiModel().getName() : s.getUpload().repoPath(),
                            verified ? s.getAiModel().getFormat() : s.getUpload().getModelFormat(),
                            verified ? s.getAiModel().getRepoUrl() : s.getUpload().getGithubUrl(),
                            verified ? null : s.getUpload().getId());
                })
                .orElse(null);
    }

    private void sendDeploy(SessionRuntime runtime, DeployTarget target) {
        UUID transferId = UUID.randomUUID();
        runtime.setTransferId(transferId);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("source", target.source());
        model.put("name", target.modelName());
        model.put("format", target.format());
        model.put("transferId", transferId.toString());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", target.sessionId());
        data.put("serialCode", target.serialCode());
        data.put("boardType", target.boardType());
        data.put("model", model);

        Optional<String> messageId = relaySender.sendRequest(runtime.getPiId(), RelayMessageType.DEPLOY, data);
        if (messageId.isEmpty()) {
            fail(runtime, DeployStep.TRANSFER, "릴레이 Pi가 연결되어 있지 않습니다.");
            return;
        }
        runtime.setDeployMessageId(messageId.get());
        runtime.setDeploySent(true);
        runtime.setPhase(SessionPhase.DEPLOYING);
        progress(runtime, DeployStep.TRANSFER, StepState.STARTED, null);
        sendFile(runtime);
    }

    private void sendFile(SessionRuntime runtime) {
        runtime.incrementFileAttempts();
        long size = runtime.getSizeBytes();
        int chunkCount = (int) ((size + CHUNK_SIZE - 1) / CHUNK_SIZE);
        Long piId = runtime.getPiId();
        UUID transferId = runtime.getTransferId();

        Map<String, Object> begin = new LinkedHashMap<>();
        begin.put("transferId", transferId.toString());
        begin.put("fileName", "model.zip");
        begin.put("sizeBytes", size);
        begin.put("sha256", runtime.getSha256());
        begin.put("chunkSize", CHUNK_SIZE);
        begin.put("chunkCount", chunkCount);

        boolean ok = relaySender.send(piId, RelayMessageType.FILE_BEGIN, begin);
        try (InputStream in = Files.newInputStream(runtime.getFile())) {
            byte[] buffer = new byte[CHUNK_SIZE];
            for (int seq = 0; ok && seq < chunkCount; seq++) {
                int n = in.readNBytes(buffer, 0, CHUNK_SIZE);
                ByteBuffer frame = ByteBuffer.allocate(20 + n);
                frame.putLong(transferId.getMostSignificantBits());
                frame.putLong(transferId.getLeastSignificantBits());
                frame.putInt(seq);
                frame.put(buffer, 0, n);
                ok = relaySender.sendBinary(piId, frame.array());
            }
        } catch (IOException | NullPointerException e) {
            ok = false;
        }
        ok = ok && relaySender.send(piId, RelayMessageType.FILE_END, Map.of("transferId", transferId.toString()));
        if (!ok) {
            fail(runtime, DeployStep.TRANSFER, "릴레이 Pi로 파일을 보내지 못했습니다.");
            return;
        }
        runtime.setDeadline(Instant.now().plus(FILE_ACK_TIMEOUT));
    }

    // L2의 file_ack. 실패면 한 번만 다시 보낸다.
    public void onFileAck(SessionRuntime runtime, boolean ok, String error) {
        if (ok) {
            runtime.deleteFile();
            runtime.setDeadline(Instant.now().plus(RUN_TIMEOUT));
            return;
        }
        if (runtime.getFileAttempts() < MAX_FILE_ATTEMPTS && runtime.getFile() != null) {
            log.info("세션 {} 파일 검증 실패({}), 다시 보낸다", runtime.getSessionId(), error);
            backgroundTasks.run("resend file " + runtime.getSessionId(), () -> sendFile(runtime));
            return;
        }
        fail(runtime, DeployStep.TRANSFER, "파일 전송 검증에 실패했습니다 (" + error + ").");
    }

    // L2의 deploy_progress를 화면에 전달한다. run 완료면 실행 중으로 보고 유휴 타이머를 시작한다.
    public void onProgress(SessionRuntime runtime, DeployStep step, StepState state, String message) {
        progress(runtime, step, state, message);
        if (step == DeployStep.RUN && state == StepState.DONE) {
            runtime.setPhase(SessionPhase.RUNNING);
            runtime.setDeadline(null);
            lifecycleService.touch(runtime.getSessionId());
        }
    }

    public void fail(SessionRuntime runtime, DeployStep step, String message) {
        progress(runtime, step, StepState.FAILED, message);
        lifecycleService.end(runtime.getSessionId(), EndReason.FAILED, message, true);
    }

    private void progress(SessionRuntime runtime, DeployStep step, StepState state, String message) {
        runtime.updateStep(step, state, message);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", runtime.getSessionId());
        data.put("step", step.getWire());
        data.put("state", state);
        data.put("message", message);
        clientPushService.push(runtime.getUserId(), PushType.DEPLOY_PROGRESS, data);
    }
}
