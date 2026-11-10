package com.crimson.auth.service.TickectingService;

import com.crimson.auth.entity.ticket.Payment;
import com.crimson.auth.entity.ticket.PaymentStatus;
import com.crimson.auth.entity.ticket.Reservation;
import com.crimson.auth.entity.ticket.ReservationStatus;
import com.crimson.auth.exception.TicketingException;
import com.crimson.auth.repository.ticketrepository.PaymentRepository;
import com.crimson.auth.repository.ticketrepository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentSettlementService {
    private static final Logger log = LoggerFactory.getLogger(PaymentSettlementService.class);

    private final PaymentRepository payments;
    private final ReservationRepository reservations;
    private final InventoryService inventory;
    private final TicketService ticketService;
    private final PaymentGateway gateway;

    @Transactional
    public Payment settle(UUID paymentId, String providerRef, PaymentOutcome outcome, String failureReason) {
        UUID reservationId = payments.findReservationId(paymentId)
                .orElseThrow(() -> TicketingException.notFound("Payment not found"));
        Reservation r = reservations.findByIdForUpdate(reservationId)
                .orElseThrow(() -> TicketingException.notFound("Reservation not found"));
        Payment p = payments.findByIdForUpdate(paymentId)
                .orElseThrow(() -> TicketingException.notFound("Payment not found"));

        if (providerRef != null && p.getProviderRef() == null) {
            p.setProviderRef(providerRef);
        }
        switch (outcome) {
            case PENDING -> {
                if (p.getStatus() == PaymentStatus.INITIATED) {
                    p.setStatus(PaymentStatus.PENDING);
                }
            }
            case FAILED -> fail(r, p, failureReason);
            case SUCCEEDED -> succeed(r, p);
        }
        return p;
    }

    private void fail(Reservation r, Payment p, String reason) {
        if (!PaymentStatus.IN_FLIGHT.contains(p.getStatus())) {
            return;
        }
        p.setStatus(PaymentStatus.FAILED);
        p.setFailureReason(truncate(reason));
        if (r.getStatus() == ReservationStatus.PAYMENT_PENDING) {
            r.setStatus(ReservationStatus.HELD); // customer may retry until the hold expires
        }
    }

    private void succeed(Reservation r, Payment p) {
        PaymentStatus s = p.getStatus();
        if (s == PaymentStatus.SUCCEEDED || s == PaymentStatus.REFUND_PENDING || s == PaymentStatus.REFUNDED) {
        }
        switch (r.getStatus()) {
            case CONFIRMED -> refundLater(p, "Reservation was already paid by another payment");
            case HELD, PAYMENT_PENDING -> confirm(r, p);
            case EXPIRED, CANCELLED -> {
                // Payment landed after we released the stock: take it back if anyone left any.
                if (inventory.tryReacquire(r.getId())) {
                    log.warn("Late payment {} re-acquired stock for {} reservation {}", p.getId(), r.getStatus(), r.getId());
                    confirm(r, p);
                } else {
                    refundLater(p, "Hold expired and the tickets were sold to someone else");
                }
            }
        }
    }

    private void confirm(Reservation r, Payment p) {
        r.setStatus(ReservationStatus.CONFIRMED);
        r.setConfirmedAt(Instant.now());
        p.setStatus(PaymentStatus.SUCCEEDED);
        p.setFailureReason(null);
        ticketService.issueFor(r);
    }

    private void refundLater(Payment p, String reason) {
        log.warn("Payment {} will be refunded: {}", p.getId(), reason);
        p.setStatus(PaymentStatus.REFUND_PENDING);
        p.setFailureReason(truncate(reason));
    }


    @Transactional
    public void handleWebhook(String provider, WebhookEvent ev) {
        if (payments.recordWebhookEvent(provider, ev.eventId()) == 0) {
            log.debug("Duplicate webhook {} ignored", ev.eventId());
            return;
        }
        Optional<Payment> payment = ev.providerRef() == null ? Optional.empty()
                : payments.findByProviderAndProviderRef(provider, ev.providerRef());
        if (payment.isEmpty() && ev.paymentId() != null) {
            payment = payments.findById(UUID.fromString(ev.paymentId()));
        }
        Payment p = payment.orElseThrow(() -> TicketingException.unavailable("Payment not known yet, retry later"));
        settle(p.getId(), ev.providerRef(), ev.outcome(), ev.failureReason());
    }

    @Transactional
    public void refund(UUID paymentId) {
        Payment p = payments.findByIdForUpdateSkipLocked(paymentId).orElse(null);
        if (p == null || p.getStatus() != PaymentStatus.REFUND_PENDING) {
            return;
        }
        if (p.getProviderRef() == null) {
            log.error("Payment {} needs a refund but has no provider reference; manual action required", p.getId());
            return;
        }
        try {
            gateway.refund(new RefundRequest(p.getProviderRef(), p.getAmountCents(), p.getCurrency(), "refund-" + p.getId()));
            p.setStatus(PaymentStatus.REFUNDED);
        } catch (PaymentGatewayException e) {
            p.setRefundAttempts(p.getRefundAttempts() + 1);
            log.error("Refund attempt {} failed for payment {}: {}", p.getRefundAttempts(), p.getId(), e.getMessage());
        }
    }

    private static String truncate(String s) {
        return s == null ? null : s.substring(0, Math.min(s.length(), 255));
    }
}
