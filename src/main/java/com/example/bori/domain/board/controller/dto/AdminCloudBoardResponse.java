package com.example.bori.domain.board.controller.dto;

import com.example.bori.domain.board.domain.BoardStatus;
import com.example.bori.domain.board.domain.BoardType;
import com.example.bori.domain.board.domain.CloudBoard;
import com.example.bori.domain.model.domain.AiModel;
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
        List<Long> modelIds
) {

    public static AdminCloudBoardResponse from(CloudBoard board) {
        return new AdminCloudBoardResponse(
                board.getId(), board.getName(), board.getSpec(), board.getBoardType(), board.getHost(),
                board.getPi().getId(), board.getPi().getName(), board.getUsbSlot(), board.getSerialCode(),
                board.getUploadLimitMb(), board.isExecuteAble(), board.isOnline(), board.getStatus(),
                board.getModels().stream().map(AiModel::getId).sorted(Comparator.naturalOrder()).toList());
    }
}
