package com.example.bori.domain.board.controller.dto;

import com.example.bori.domain.board.domain.BoardType;
import com.example.bori.domain.board.domain.CloudAvailability;
import java.util.List;

public record CloudBoardResponse(
        Long id,
        String name,
        String host,
        BoardType boardType,
        String spec,
        String piName,
        String usbSlot,
        int uploadLimitMb,
        boolean online,
        CloudAvailability availability,
        int queueCount,
        int queueCapacity,
        int totalRuns,
        List<ModelStat> models
) {

    // avgFps·avgAccuracy·runCount는 이 보드에서 실행한 기록이 없으면 0
    public record ModelStat(
            Long id,
            String name,
            String format,
            String description,
            double avgFps,
            double avgAccuracy,
            int runCount
    ) {
    }
}
