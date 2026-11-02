package com.example.bori.domain.rental.controller;

import com.example.bori.domain.rental.controller.dto.AdminRentalBoardResponse;
import com.example.bori.domain.rental.controller.dto.AdminRentalResponse;
import com.example.bori.domain.rental.controller.dto.RentalBoardRequest;
import com.example.bori.domain.rental.controller.dto.RentalBoardStatusRequest;
import com.example.bori.domain.rental.service.AdminRentalService;
import com.example.bori.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminRentalController {

    private final AdminRentalService adminRentalService;

    @GetMapping("/rental-boards")
    public ResponseEntity<List<AdminRentalBoardResponse>> getBoards() {
        return ResponseEntity.ok(adminRentalService.getBoards());
    }

    @PostMapping("/rental-boards")
    public ResponseEntity<AdminRentalBoardResponse> create(@AuthenticationPrincipal CustomUserDetails admin,
                                                           @Valid @RequestBody RentalBoardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminRentalService.create(admin.getUserId(), request));
    }

    @PutMapping("/rental-boards/{boardId}")
    public ResponseEntity<AdminRentalBoardResponse> update(@AuthenticationPrincipal CustomUserDetails admin,
                                                           @PathVariable Long boardId,
                                                           @Valid @RequestBody RentalBoardRequest request) {
        return ResponseEntity.ok(adminRentalService.update(admin.getUserId(), boardId, request));
    }

    @PostMapping(value = "/rental-boards/{boardId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminRentalBoardResponse> uploadPhoto(@AuthenticationPrincipal CustomUserDetails admin,
                                                                @PathVariable Long boardId,
                                                                @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(adminRentalService.uploadPhoto(admin.getUserId(), boardId, file));
    }

    @PatchMapping("/rental-boards/{boardId}/status")
    public ResponseEntity<AdminRentalBoardResponse> changeStatus(@AuthenticationPrincipal CustomUserDetails admin,
                                                                 @PathVariable Long boardId,
                                                                 @Valid @RequestBody RentalBoardStatusRequest request) {
        return ResponseEntity.ok(adminRentalService.changeStatus(admin.getUserId(), boardId, request.status()));
    }

    @DeleteMapping("/rental-boards/{boardId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails admin, @PathVariable Long boardId) {
        adminRentalService.delete(admin.getUserId(), boardId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rentals/return-requests")
    public ResponseEntity<List<AdminRentalResponse>> getReturnRequests() {
        return ResponseEntity.ok(adminRentalService.getReturnRequests());
    }

    @PostMapping("/rentals/{rentalId}/approve")
    public ResponseEntity<AdminRentalResponse> approveReturn(@AuthenticationPrincipal CustomUserDetails admin,
                                                             @PathVariable Long rentalId) {
        return ResponseEntity.ok(adminRentalService.approveReturn(admin.getUserId(), rentalId));
    }
}
