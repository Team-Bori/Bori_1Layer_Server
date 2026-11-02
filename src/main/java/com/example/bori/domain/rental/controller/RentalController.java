package com.example.bori.domain.rental.controller;

import com.example.bori.domain.rental.controller.dto.MyRentalResponse;
import com.example.bori.domain.rental.controller.dto.RentalBoardResponse;
import com.example.bori.domain.rental.domain.RentalBoardStatus;
import com.example.bori.domain.rental.service.RentalService;
import com.example.bori.global.security.CustomUserDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rental")
public class RentalController {

    private final RentalService rentalService;

    @GetMapping("/boards")
    public ResponseEntity<List<RentalBoardResponse>> getBoards(@RequestParam(required = false) RentalBoardStatus status) {
        return ResponseEntity.ok(rentalService.getBoards(status));
    }

    @PostMapping("/boards/{boardId}/rent")
    public ResponseEntity<MyRentalResponse> rent(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long boardId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rentalService.rent(user.getUserId(), boardId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<MyRentalResponse>> getMyRentals(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(rentalService.getMyRentals(user.getUserId()));
    }

    @PostMapping("/rentals/{rentalId}/return")
    public ResponseEntity<MyRentalResponse> requestReturn(@AuthenticationPrincipal CustomUserDetails user,
                                                          @PathVariable Long rentalId) {
        return ResponseEntity.ok(rentalService.requestReturn(user.getUserId(), rentalId));
    }
}
