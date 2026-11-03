package com.crimson.auth.dto.response;

import com.crimson.auth.entity.ticket.Payment;
import com.crimson.auth.entity.ticket.PaymentStatus;

import java.util.UUID;

public record PaymentResponse(UUID paymentId, UUID reservationId, PaymentStatus status, long amountCents, String currency, String failureReason) {
    public static PaymentResponse of(Payment p) {
        return new PaymentResponse(p.getId(), p.getReservationId(), p.getStatus(), p.getAmountCents(),
                p.getCurrency(), p.getFailureReason());
    }
}
