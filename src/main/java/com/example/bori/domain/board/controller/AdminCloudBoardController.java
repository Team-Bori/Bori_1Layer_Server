package com.example.bori.domain.board.controller;

import com.example.bori.domain.board.controller.dto.AdminCloudBoardResponse;
import com.example.bori.domain.board.controller.dto.BoardStatusRequest;
import com.example.bori.domain.board.controller.dto.CloudBoardRequest;
import com.example.bori.domain.board.controller.dto.UploadLimitRequest;
import com.example.bori.domain.board.service.AdminCloudBoardService;
import com.example.bori.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/cloud-boards")
public class AdminCloudBoardController {

    private final AdminCloudBoardService adminCloudBoardService;

    @GetMapping
    public ResponseEntity<List<AdminCloudBoardResponse>> getBoards() {
        return ResponseEntity.ok(adminCloudBoardService.getBoards());
    }

    @PostMapping
    public ResponseEntity<AdminCloudBoardResponse> create(@AuthenticationPrincipal CustomUserDetails admin,
                                                          @Valid @RequestBody CloudBoardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCloudBoardService.create(admin.getUserId(), request));
    }

    @PutMapping("/{boardId}")
    public ResponseEntity<AdminCloudBoardResponse> update(@AuthenticationPrincipal CustomUserDetails admin,
                                                          @PathVariable Long boardId,
                                                          @Valid @RequestBody CloudBoardRequest request) {
        return ResponseEntity.ok(adminCloudBoardService.update(admin.getUserId(), boardId, request));
    }

    @PatchMapping("/{boardId}/status")
    public ResponseEntity<AdminCloudBoardResponse> changeStatus(@AuthenticationPrincipal CustomUserDetails admin,
                                                                @PathVariable Long boardId,
                                                                @Valid @RequestBody BoardStatusRequest request) {
        return ResponseEntity.ok(adminCloudBoardService.changeStatus(admin.getUserId(), boardId, request.status()));
    }

    @PatchMapping("/{boardId}/upload-limit")
    public ResponseEntity<AdminCloudBoardResponse> changeUploadLimit(@AuthenticationPrincipal CustomUserDetails admin,
                                                                     @PathVariable Long boardId,
                                                                     @Valid @RequestBody UploadLimitRequest request) {
        return ResponseEntity.ok(adminCloudBoardService.changeUploadLimit(admin.getUserId(), boardId, request.uploadLimitMb()));
    }

    @PostMapping("/{boardId}/force-end")
    public ResponseEntity<Void> forceEnd(@AuthenticationPrincipal CustomUserDetails admin, @PathVariable Long boardId) {
        adminCloudBoardService.forceEnd(admin.getUserId(), boardId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails admin, @PathVariable Long boardId) {
        adminCloudBoardService.delete(admin.getUserId(), boardId);
        return ResponseEntity.noContent().build();
    }
}
