package com.crimson.auth.controller.ticketing;

import com.crimson.auth.dto.request.CheckInRequest;
import com.crimson.auth.dto.response.MessageResponse;
import com.crimson.auth.dto.response.TicketResponse;
import com.crimson.auth.security.UserDetailsImpl;
import com.crimson.auth.service.TickectingService.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @GetMapping("/api/tickets/me")
    public ResponseEntity<List<TicketResponse>> mine(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(ticketService.listForUser(user.getId()));
    }

    /** Gate scanner endpoint. SecurityConfig: /api/moderator/** => ADMIN or MODERATOR. */
    @PostMapping("/api/moderator/tickets/check-in")
    public ResponseEntity<MessageResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        ticketService.checkIn(request);
        return ResponseEntity.ok(new MessageResponse("Checked in"));
    }
}
