package com.example.bori.domain.board.controller;

import com.example.bori.domain.board.controller.dto.CloudBoardResponse;
import com.example.bori.domain.board.domain.BoardType;
import com.example.bori.domain.board.service.CloudBoardQueryService;
import com.example.bori.global.security.CustomUserDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cloud/boards")
public class CloudBoardController {

    private final CloudBoardQueryService cloudBoardQueryService;

    @GetMapping
    public ResponseEntity<List<CloudBoardResponse>> getBoards(@AuthenticationPrincipal CustomUserDetails user,
                                                              @RequestParam(required = false) BoardType type,
                                                              @RequestParam(required = false) String q) {
        return ResponseEntity.ok(cloudBoardQueryService.getBoards(user.getUserId(), type, q));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<CloudBoardResponse> getBoard(@AuthenticationPrincipal CustomUserDetails user,
                                                       @PathVariable Long boardId) {
        return ResponseEntity.ok(cloudBoardQueryService.getBoard(user.getUserId(), boardId));
    }
}
