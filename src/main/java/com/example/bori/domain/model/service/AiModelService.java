package com.example.bori.domain.model.service;

import com.example.bori.domain.adminlog.domain.AdminAction;
import com.example.bori.domain.adminlog.domain.TargetType;
import com.example.bori.domain.adminlog.service.AdminActionLogService;
import com.example.bori.domain.model.controller.dto.AiModelRequest;
import com.example.bori.domain.model.controller.dto.AiModelResponse;
import com.example.bori.domain.model.domain.AiModel;
import com.example.bori.domain.model.domain.AiModelRepository;
import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;
import com.example.bori.global.github.GithubRepoUrl;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiModelService {

    private final AiModelRepository aiModelRepository;
    private final AdminActionLogService adminActionLogService;

    public List<AiModelResponse> getModels() {
        return aiModelRepository.findAll(Sort.by("id")).stream()
                .map(AiModelResponse::from)
                .toList();
    }

    @Transactional
    public AiModelResponse create(Long adminId, AiModelRequest request) {
        String repoUrl = GithubRepoUrl.parse(request.repoUrl()).canonical();
        AiModel model = aiModelRepository.save(new AiModel(request.name(), request.format(), request.description(), repoUrl));
        adminActionLogService.record(adminId, AdminAction.CREATE, TargetType.AI_MODEL, model.getId(), model.getName() + " 등록");
        return AiModelResponse.from(model);
    }

    @Transactional
    public AiModelResponse update(Long adminId, Long modelId, AiModelRequest request) {
        AiModel model = getModel(modelId);
        String repoUrl = GithubRepoUrl.parse(request.repoUrl()).canonical();
        model.update(request.name(), request.format(), request.description(), repoUrl);
        adminActionLogService.record(adminId, AdminAction.UPDATE, TargetType.AI_MODEL, model.getId(), model.getName() + " 정보 수정");
        return AiModelResponse.from(model);
    }

    public AiModel getModel(Long modelId) {
        return aiModelRepository.findById(modelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));
    }
}
