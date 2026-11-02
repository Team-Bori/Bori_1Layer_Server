package com.example.bori.domain.metric.controller.dto;

import java.time.LocalDateTime;

public record UploadHistoryResponse(
        Long id,
        String githubUrl,
        String modelFormat,
        int sizeMb,
        Long boardId,
        String boardName,
        int uploadLimitMb,
        LocalDateTime createdAt
) {
}
