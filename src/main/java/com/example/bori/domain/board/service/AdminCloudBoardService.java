package com.example.bori.domain.board.service;

import com.example.bori.domain.adminlog.domain.AdminAction;
import com.example.bori.domain.adminlog.domain.TargetType;
import com.example.bori.domain.adminlog.service.AdminActionLogService;
import com.example.bori.domain.board.controller.dto.AdminCloudBoardResponse;
import com.example.bori.domain.board.controller.dto.CloudBoardRequest;
import com.example.bori.domain.board.domain.BoardStatus;
import com.example.bori.domain.board.domain.CloudBoard;
import com.example.bori.domain.board.domain.CloudBoardRepository;
import com.example.bori.domain.model.domain.AiModel;
import com.example.bori.domain.model.domain.AiModelRepository;
import com.example.bori.domain.pi.domain.HostPi;
import com.example.bori.domain.pi.domain.HostPiRepository;
import com.example.bori.domain.pi.service.HostPiService;
import com.example.bori.domain.queue.service.QueueService;
import com.example.bori.domain.relay.socket.RelayConnectionRegistry;
import com.example.bori.domain.session.domain.BoardSession;
import com.example.bori.domain.session.domain.BoardSessionRepository;
import com.example.bori.domain.session.domain.EndReason;
import com.example.bori.domain.session.service.SessionLifecycleService;
import com.example.bori.domain.user.domain.User;
import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCloudBoardService {

    private final CloudBoardRepository cloudBoardRepository;
    private final AiModelRepository aiModelRepository;
    private final HostPiService hostPiService;
    private final AdminActionLogService adminActionLogService;
    private final RelayConnectionRegistry relayConnectionRegistry;
    private final HostPiRepository hostPiRepository;
    private final BoardSessionRepository boardSessionRepository;
    private final SessionLifecycleService lifecycleService;
    private final QueueService queueService;

    public List<AdminCloudBoardResponse> getBoards() {
        Map<Long, BoardSession> active = boardSessionRepository.findAllByEndedAtIsNull().stream()
                .collect(Collectors.toMap(s -> s.getCloudBoard().getId(), Function.identity()));
        return cloudBoardRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .map(board -> AdminCloudBoardResponse.of(board, currentSession(active.get(board.getId()))))
                .toList();
    }

    // 진행 중 세션 강제 종료. 학생 화면에는 강제 종료로 기록된다.
    @Transactional
    public void forceEnd(Long adminId, Long boardId) {
        CloudBoard board = getBoard(boardId);
        BoardSession session = boardSessionRepository.findByCloudBoardIdAndEndedAtIsNull(boardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = session.getUser();
        lifecycleService.end(session.getId(), EndReason.FORCED, "관리자가 세션을 종료했습니다.", true);
        adminActionLogService.record(adminId, AdminAction.FORCE_END, TargetType.BOARD_SESSION, session.getId(),
                "%s · %s (%s)".formatted(board.getName(), user.getName(), user.getStudentNo()));
    }

    // 등록 직후엔 오프라인이다. 릴레이 Pi가 이 시리얼을 보고하면 온라인으로 바뀐다.
    @Transactional
    public AdminCloudBoardResponse create(Long adminId, CloudBoardRequest request) {
        if (cloudBoardRepository.existsBySerialCodeAndDeletedAtIsNull(request.serialCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_BOARD_SERIAL);
        }
        if (cloudBoardRepository.existsByPiIdAndUsbSlotAndDeletedAtIsNull(request.piId(), request.usbSlot())) {
            throw new BusinessException(ErrorCode.DUPLICATE_BOARD_SLOT);
        }
        HostPi pi = hostPiService.getPi(request.piId());
        CloudBoard board = cloudBoardRepository.save(new CloudBoard(
                pi, request.name().trim(), request.spec(), request.boardType(), request.host(), request.uploadLimitMb(),
                request.serialCode(), request.usbSlot(), request.executeAble(), findModels(request.modelIds())));
        syncWithRelay(board);
        adminActionLogService.record(adminId, AdminAction.CREATE, TargetType.CLOUD_BOARD, board.getId(), board.getName() + " 등록");
        return AdminCloudBoardResponse.from(board);
    }

    @Transactional
    public AdminCloudBoardResponse update(Long adminId, Long boardId, CloudBoardRequest request) {
        CloudBoard board = getBoard(boardId);
        if (cloudBoardRepository.existsBySerialCodeAndDeletedAtIsNullAndIdNot(request.serialCode(), boardId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_BOARD_SERIAL);
        }
        if (cloudBoardRepository.existsByPiIdAndUsbSlotAndDeletedAtIsNullAndIdNot(request.piId(), request.usbSlot(), boardId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_BOARD_SLOT);
        }
        HostPi pi = hostPiService.getPi(request.piId());
        board.update(pi, request.name().trim(), request.spec(), request.boardType(), request.host(), request.uploadLimitMb(),
                request.serialCode(), request.usbSlot(), request.executeAble(), findModels(request.modelIds()));
        syncWithRelay(board);
        adminActionLogService.record(adminId, AdminAction.UPDATE, TargetType.CLOUD_BOARD, board.getId(), board.getName() + " 정보 수정");
        return AdminCloudBoardResponse.from(board);
    }

    @Transactional
    public AdminCloudBoardResponse changeStatus(Long adminId, Long boardId, BoardStatus status) {
        CloudBoard board = getBoard(boardId);
        BoardStatus before = board.getStatus();
        if (before != status) {
            board.changeStatus(status);
            adminActionLogService.record(adminId, AdminAction.STATUS_CHANGE, TargetType.CLOUD_BOARD, board.getId(),
                    "%s: %s → %s".formatted(board.getName(), before.getLabel(), status.getLabel()));
        }
        return AdminCloudBoardResponse.from(board);
    }

    @Transactional
    public AdminCloudBoardResponse changeUploadLimit(Long adminId, Long boardId, int uploadLimitMb) {
        CloudBoard board = getBoard(boardId);
        int before = board.getUploadLimitMb();
        if (before != uploadLimitMb) {
            board.changeUploadLimit(uploadLimitMb);
            adminActionLogService.record(adminId, AdminAction.LIMIT_CHANGE, TargetType.CLOUD_BOARD, board.getId(),
                    "%s: 업로드 한도 %d → %d MB".formatted(board.getName(), before, uploadLimitMb));
        }
        return AdminCloudBoardResponse.from(board);
    }

    // 사용 중이면 세션도 끊고, 대기는 모두 취소한다. 지난 이력은 남는다.
    @Transactional
    public void delete(Long adminId, Long boardId) {
        CloudBoard board = getBoard(boardId);
        boardSessionRepository.findByCloudBoardIdAndEndedAtIsNull(boardId)
                .ifPresent(s -> lifecycleService.end(s.getId(), EndReason.FORCED, "보드가 삭제되었습니다.", true));
        queueService.cancelAll(boardId);
        board.delete(LocalDateTime.now());
        adminActionLogService.record(adminId, AdminAction.DELETE, TargetType.CLOUD_BOARD, board.getId(), board.getName() + " 삭제");
    }

    // 이미 어떤 파이가 이 시리얼을 보고했다면 그 위치로 바로 온라인. 아니면 오프라인.
    private void syncWithRelay(CloudBoard board) {
        relayConnectionRegistry.findReporting(board.getSerialCode()).ifPresentOrElse(
                connection -> board.attach(hostPiRepository.getReferenceById(connection.getPiId()),
                        connection.reportedSlot(board.getSerialCode()).orElse(null)),
                board::markOffline);
    }

    private AdminCloudBoardResponse.CurrentSession currentSession(BoardSession s) {
        if (s == null) {
            return null;
        }
        return new AdminCloudBoardResponse.CurrentSession(s.getId(), s.getUser().getName(), s.getUser().getStudentNo(),
                s.modelLabel(), s.getStartedAt());
    }

    private CloudBoard getBoard(Long boardId) {
        return cloudBoardRepository.findByIdAndDeletedAtIsNull(boardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));
    }

    private List<AiModel> findModels(List<Long> modelIds) {
        if (modelIds == null || modelIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinct = modelIds.stream().distinct().toList();
        List<AiModel> models = aiModelRepository.findAllById(distinct);
        if (models.size() != distinct.size()) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        return models;
    }
}
