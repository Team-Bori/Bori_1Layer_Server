package com.example.bori.domain.model.controller;

import com.example.bori.domain.model.controller.dto.AiModelRequest;
import com.example.bori.domain.model.controller.dto.AiModelResponse;
import com.example.bori.domain.model.service.AiModelService;
import com.example.bori.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/models")
public class AdminAiModelController {

    private final AiModelService aiModelService;

    @GetMapping
    public ResponseEntity<List<AiModelResponse>> getModels() {
        return ResponseEntity.ok(aiModelService.getModels());
    }

    @PostMapping
    public ResponseEntity<AiModelResponse> create(@AuthenticationPrincipal CustomUserDetails admin,
                                                  @Valid @RequestBody AiModelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aiModelService.create(admin.getUserId(), request));
    }

    @PutMapping("/{modelId}")
    public ResponseEntity<AiModelResponse> update(@AuthenticationPrincipal CustomUserDetails admin,
                                                  @PathVariable Long modelId,
                                                  @Valid @RequestBody AiModelRequest request) {
        return ResponseEntity.ok(aiModelService.update(admin.getUserId(), modelId, request));
    }
}
