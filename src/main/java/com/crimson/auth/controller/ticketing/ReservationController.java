package com.crimson.auth.controller.ticketing;

import com.crimson.auth.dto.request.CreateReservationRequest;
import com.crimson.auth.dto.response.MessageResponse;
import com.crimson.auth.dto.response.ReservationResponse;
import com.crimson.auth.security.UserDetailsImpl;
import com.crimson.auth.service.TickectingService.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@AuthenticationPrincipal UserDetailsImpl user,
                                                      @RequestHeader("Idempotency-Key") String key,
                                                      @Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse body = reservationService.reserve(user.getId(), IdempotencyKeys.require(key), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> get(@AuthenticationPrincipal UserDetailsImpl user, @PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.get(user.getId(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> cancel(@AuthenticationPrincipal UserDetailsImpl user, @PathVariable UUID id) {
        reservationService.cancel(user.getId(), id);
        return ResponseEntity.ok(new MessageResponse("Reservation cancelled"));
    }
}