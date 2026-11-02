package com.example.bori.domain.session.service;

import com.example.bori.domain.board.domain.CloudBoard;
import com.example.bori.domain.board.domain.CloudBoardRepository;
import com.example.bori.domain.model.domain.AiModel;
import com.example.bori.domain.model.domain.AiModelRepository;
import com.example.bori.domain.queue.domain.QueueEntry;
import com.example.bori.domain.queue.domain.QueueEntryRepository;
import com.example.bori.domain.queue.domain.QueueState;
import com.example.bori.domain.queue.service.QueueService;
import com.example.bori.domain.relay.socket.RelayConnectionRegistry;
import com.example.bori.domain.session.controller.dto.CloudMeResponse;
import com.example.bori.domain.session.controller.dto.MySessionResponse;
import com.example.bori.domain.session.controller.dto.SessionStartRequest;
import com.example.bori.domain.session.deploy.DeployService;
import com.example.bori.domain.session.domain.BoardSession;
import com.example.bori.domain.session.domain.BoardSessionRepository;
import com.example.bori.domain.session.domain.EndReason;
import com.example.bori.domain.session.domain.UserModelUpload;
import com.example.bori.domain.session.domain.UserModelUploadRepository;
import com.example.bori.domain.session.runtime.SessionPhase;
import com.example.bori.domain.session.runtime.SessionRuntime;
import com.example.bori.domain.session.runtime.SessionRuntimeRegistry;
import com.example.bori.domain.user.domain.User;
import com.example.bori.domain.user.domain.UserRepository;
import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;
import com.example.bori.global.github.GithubRepoUrl;
import com.example.bori.global.tx.AfterCommit;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CloudSessionService {

    private final BoardSessionRepository boardSessionRepository;
    private final CloudBoardRepository cloudBoardRepository;
    private final AiModelRepository aiModelRepository;
    private final UserModelUploadRepository uploadRepository;
    private final UserRepository userRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final QueueService queueService;
    private final RelayConnectionRegistry relayConnectionRegistry;
    private final SessionRuntimeRegistry runtimeRegistry;
    private final SessionLifecycleService lifecycleService;
    private final DeployService deployService;

    // 접속 + 배포 시작. 보드 행을 잠가서 같은 보드에 동시에 두 세션이 생기지 않게 한다.
    @Transactional
    public MySessionResponse start(Long userId, Long boardId, SessionStartRequest request) {
        if (!request.isVerified() && !request.isUpload()) {
            throw new BusinessException(ErrorCode.INVALID_MODEL_SOURCE);
        }
        CloudBoard board = cloudBoardRepository.findForUpdate(boardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));
        if (boardSessionRepository.findByUserIdAndEndedAtIsNull(userId).isPresent()) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ACTIVE);
        }
        if (!board.isAccessible() || !relayConnectionRegistry.isConnected(board.getPi().getId())) {
            throw new BusinessException(ErrorCode.BOARD_UNAVAILABLE);
        }
        if (boardSessionRepository.existsByCloudBoardIdAndEndedAtIsNull(boardId)) {
            throw new BusinessException(ErrorCode.BOARD_BUSY);
        }
        checkQueue(userId, boardId);

        User user = userRepository.getReferenceById(userId);
        LocalDateTime now = LocalDateTime.now();
        BoardSession session;
        if (request.isVerified()) {
            AiModel model = aiModelRepository.findById(request.aiModelId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));
            if (!board.supports(model.getId())) {
                throw new BusinessException(ErrorCode.MODEL_NOT_SUPPORTED);
            }
            session = BoardSession.withVerifiedModel(user, board, model, now);
        } else {
            String url = GithubRepoUrl.parse(request.githubUrl()).canonical();
            if (request.sizeMb() > board.getUploadLimitMb()) {
                throw new BusinessException(ErrorCode.UPLOAD_LIMIT_EXCEEDED);
            }
            UserModelUpload upload = uploadRepository.save(
                    new UserModelUpload(user, board, url, request.modelFormat().trim(), request.sizeMb()));
            session = BoardSession.withUpload(user, board, upload, now);
        }
        boardSessionRepository.saveAndFlush(session);

        SessionRuntime runtime = new SessionRuntime(session.getId(), userId, board.getPi().getId(), board.getId(),
                board.getSerialCode(), SessionPhase.FETCHING);
        AfterCommit.run(() -> {
            runtimeRegistry.put(runtime);
            deployService.start(runtime.getSessionId());
        });
        return toResponse(session, runtime);
    }

    // 대기열 규칙: 내 차례(READY)인 보드만 접속할 수 있고, 다른 사람이 대기 중인 보드엔 끼어들 수 없다.
    private void checkQueue(Long userId, Long boardId) {
        Optional<QueueEntry> mine = queueEntryRepository.findByUserIdAndStateIn(userId, QueueState.ACTIVE);
        if (mine.isPresent()) {
            QueueEntry entry = mine.get();
            if (!entry.getCloudBoard().getId().equals(boardId)) {
                throw new BusinessException(ErrorCode.ALREADY_QUEUED);
            }
            if (entry.getState() != QueueState.READY) {
                throw new BusinessException(ErrorCode.BOARD_RESERVED);
            }
            queueService.enter(entry);
            return;
        }
        if (!queueService.activeEntries(boardId).isEmpty()) {
            throw new BusinessException(ErrorCode.BOARD_RESERVED);
        }
    }

    @Transactional
    public void end(Long userId, Long sessionId) {
        BoardSession session = getMyActiveSession(userId, sessionId);
        lifecycleService.end(session.getId(), EndReason.NORMAL, null, true);
    }

    // 화면 클릭·키 입력. 프론트는 너무 자주 보내지 않게 묶어서 보낸다 (예: 30초에 한 번).
    @Transactional
    public void activity(Long userId, Long sessionId) {
        getMyActiveSession(userId, sessionId).touch(LocalDateTime.now());
    }

    public CloudMeResponse getMe(Long userId) {
        MySessionResponse session = boardSessionRepository.findByUserIdAndEndedAtIsNull(userId)
                .map(s -> toResponse(s, runtimeRegistry.get(s.getId()).orElse(null)))
                .orElse(null);
        return new CloudMeResponse(session, queueService.getMyQueue(userId).orElse(null));
    }

    private BoardSession getMyActiveSession(Long userId, Long sessionId) {
        BoardSession session = boardSessionRepository.findById(sessionId)
                .filter(s -> s.getUser().getId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ENDED);
        }
        return session;
    }

    private MySessionResponse toResponse(BoardSession s, SessionRuntime runtime) {
        CloudBoard board = s.getCloudBoard();
        SessionRuntime rt = runtime != null ? runtime
                : new SessionRuntime(s.getId(), s.getUser().getId(), board.getPi().getId(), board.getId(),
                board.getSerialCode(), SessionPhase.RUNNING);
        return new MySessionResponse(s.getId(), board.getId(), board.getName(), board.getBoardType(), board.getHost(),
                board.getPi().getName(), board.getUsbSlot(), s.modelLabel(), s.getStartedAt(), s.getLastActiveAt(),
                SessionScheduler.IDLE_TIMEOUT.toSeconds(), rt.getPhase(), List.copyOf(rt.stepViews()));
    }
}
