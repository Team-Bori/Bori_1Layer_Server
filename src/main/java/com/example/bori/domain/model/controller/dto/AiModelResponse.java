package com.example.bori.domain.model.controller.dto;

import com.example.bori.domain.model.domain.AiModel;

public record AiModelResponse(
        Long id,
        String name,
        String format,
        String description,
        String repoUrl
) {

    public static AiModelResponse from(AiModel model) {
        return new AiModelResponse(model.getId(), model.getName(), model.getFormat(), model.getDescription(), model.getRepoUrl());
    }
}
