package com.crimson.auth.service.TickectingService;

import com.crimson.auth.config.TicketingProperties;
import com.crimson.auth.dto.response.PaymentResponse;
import com.crimson.auth.entity.ticket.Payment;
import com.crimson.auth.entity.ticket.PaymentStatus;
import com.crimson.auth.entity.ticket.Reservation;
import com.crimson.auth.entity.ticket.ReservationStatus;
import com.crimson.auth.exception.TicketingException;
import com.crimson.auth.payment.ChargeRequest;
import com.crimson.auth.payment.ChargeResult;
import com.crimson.auth.payment.PaymentGateway;
import com.crimson.auth.payment.PaymentGatewayException;
import com.crimson.auth.repository.ticketrepository.PaymentRepository;
import com.crimson.auth.repository.ticketrepository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final ReservationRepository reservations;
    private final PaymentSettlementService settlement;
    private final PaymentGateway gateway;
    private final TransactionTemplate tx;
    private final TicketingProperties props;

    public PaymentResponse pay(Long userId, UUID reservationId, String key, String paymentToken) {
        Payment payment = tx.execute(status -> initiate(userId, reservationId, key));

        if (payment.getStatus() != PaymentStatus.INITIATED || payment.getProviderRef() != null) {
            return PaymentResponse.of(payment);
        }

        ChargeResult result;
        try {
            result = gateway.charge(new ChargeRequest(payment.getId(), userId, payment.getAmountCents(),
                    payment.getCurrency(), "Concert tickets " + reservationId, payment.getId().toString(), paymentToken));
        } catch (PaymentGatewayException e) {
            log.warn("Gateway call failed for payment {}: {}", payment.getId(), e.getMessage());
            throw TicketingException.unavailable("Payment provider unavailable. Retry with the same Idempotency-Key.");
        }

        return PaymentResponse.of(settlement.settle(payment.getId(), result.providerRef(), result.outcome(), result.failureReason()));
    }

    private Payment initiate(Long userId, UUID reservationId, String key) {
        Reservation r = reservations.findByIdForUpdate(reservationId)
                .filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> TicketingException.notFound("Reservation not found"));

        Optional<Payment> existing = payments.findByUserIdAndIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            if (!existing.get().getReservationId().equals(reservationId)) {
                throw TicketingException.unprocessable("Idempotency-Key was already used for a different reservation");
            }
            return existing.get();
        }

        Instant now = Instant.now();
        switch (r.getStatus()) {
            case CONFIRMED -> throw TicketingException.conflict("Reservation is already paid");
            case EXPIRED, CANCELLED -> throw TicketingException.gone("Reservation is " + r.getStatus() + ". Start a new reservation.");
            case PAYMENT_PENDING -> {
                if (payments.existsByReservationIdAndStatusIn(reservationId, PaymentStatus.IN_FLIGHT)) {
                    throw TicketingException.conflict("A payment for this reservation is already in progress");
                }
            }
            case HELD -> {
                if (!r.getExpiresAt().isAfter(now)) {
                    throw TicketingException.gone("Reservation has expired. Start a new reservation.");
                }
            }
        }

        r.setStatus(ReservationStatus.PAYMENT_PENDING);
        Instant deadline = now.plus(props.paymentWindow());
        if (deadline.isAfter(r.getExpiresAt())) {
            r.setExpiresAt(deadline); // never release stock while the customer is mid-payment
        }

        return payments.saveAndFlush(Payment.builder()
                .reservationId(reservationId)
                .userId(userId)
                .provider(gateway.name())
                .amountCents(r.getTotalCents())
                .currency(r.getCurrency())
                .status(PaymentStatus.INITIATED)
                .idempotencyKey(key)
                .build());
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(Long userId, UUID paymentId) {
        return payments.findById(paymentId)
                .filter(p -> p.getUserId().equals(userId))
                .map(PaymentResponse::of)
                .orElseThrow(() -> TicketingException.notFound("Payment not found"));
    }
}
