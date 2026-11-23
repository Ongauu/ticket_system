package com.crimson.auth.controller.ticketing;

import com.crimson.auth.dto.request.PayRequest;
import com.crimson.auth.dto.response.PaymentResponse;
import com.crimson.auth.security.UserDetailsImpl;
import com.crimson.auth.service.TickectingService.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    /** 200 paid | 202 still processing (poll GET /api/payments/{id}) | 402 declined. */
    @PostMapping("/api/reservations/{id}/pay")
    public ResponseEntity<PaymentResponse> pay(@AuthenticationPrincipal UserDetailsImpl user,
                                               @PathVariable UUID id,
                                               @RequestHeader("Idempotency-Key") String key,
                                               @Valid @RequestBody(required = false) PayRequest request) {
        PaymentResponse body = paymentService.pay(user.getId(), id, IdempotencyKeys.require(key),
                request == null ? null : request.paymentToken());
        HttpStatus status = switch (body.status()) {
            case SUCCEEDED -> HttpStatus.OK;
            case FAILED -> HttpStatus.PAYMENT_REQUIRED;
            case REFUND_PENDING, REFUNDED -> HttpStatus.CONFLICT;
            default -> HttpStatus.ACCEPTED;
        };
        return ResponseEntity.status(status).body(body);
    }

    @GetMapping("/api/payments/{paymentId}")
    public ResponseEntity<PaymentResponse> get(@AuthenticationPrincipal UserDetailsImpl user, @PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.get(user.getId(), paymentId));
    }
}
