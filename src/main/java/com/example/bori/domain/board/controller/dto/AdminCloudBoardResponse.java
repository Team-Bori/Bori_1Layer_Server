package com.example.bori.domain.board.controller.dto;

import com.example.bori.domain.board.domain.BoardStatus;
import com.example.bori.domain.board.domain.BoardType;
import com.example.bori.domain.board.domain.CloudBoard;
import com.example.bori.domain.model.domain.AiModel;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record AdminCloudBoardResponse(
        Long id,
        String name,
        String spec,
        BoardType boardType,
        String host,
        Long piId,
        String piName,
        String usbSlot,
        String serialCode,
        int uploadLimitMb,
        boolean executeAble,
        boolean online,
        BoardStatus status,
        List<Long> modelIds,
        CurrentSession currentSession
) {

    // 사용 중이 아니면 null
    public record CurrentSession(Long sessionId, String userName, String studentNo, String modelLabel,
                                 LocalDateTime startedAt) {
    }

    public static AdminCloudBoardResponse from(CloudBoard board) {
        return of(board, null);
    }

    public static AdminCloudBoardResponse of(CloudBoard board, CurrentSession currentSession) {
        return new AdminCloudBoardResponse(
                board.getId(), board.getName(), board.getSpec(), board.getBoardType(), board.getHost(),
                board.getPi().getId(), board.getPi().getName(), board.getUsbSlot(), board.getSerialCode(),
                board.getUploadLimitMb(), board.isExecuteAble(), board.isOnline(), board.getStatus(),
                board.getModels().stream().map(AiModel::getId).sorted(Comparator.naturalOrder()).toList(),
                currentSession);
    }
}
