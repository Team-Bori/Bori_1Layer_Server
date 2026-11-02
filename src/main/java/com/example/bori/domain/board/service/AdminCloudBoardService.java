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
import com.example.bori.domain.pi.service.HostPiService;
import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
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

    public List<AdminCloudBoardResponse> getBoards() {
        return cloudBoardRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .map(AdminCloudBoardResponse::from)
                .toList();
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

    @Transactional
    public void delete(Long adminId, Long boardId) {
        CloudBoard board = getBoard(boardId);
        board.delete(LocalDateTime.now());
        adminActionLogService.record(adminId, AdminAction.DELETE, TargetType.CLOUD_BOARD, board.getId(), board.getName() + " 삭제");
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
