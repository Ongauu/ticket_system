package com.crimson.auth.job;

import com.crimson.auth.config.TicketingProperties;
import com.crimson.auth.entity.ticket.Payment;
import com.crimson.auth.entity.ticket.PaymentStatus;
import com.crimson.auth.payment.ChargeResult;
import com.crimson.auth.payment.PaymentGateway;
import com.crimson.auth.payment.PaymentGatewayException;
import com.crimson.auth.payment.PaymentOutcome;
import com.crimson.auth.repository.ticketrepository.PaymentRepository;
import com.crimson.auth.service.TickectingService.PaymentSettlementService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ticketing.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationJob.class);

    private final PaymentRepository payments;
    private final PaymentSettlementService settlement;
    private final PaymentGateway gateway;
    private final TicketingProperties props;

    @Scheduled(fixedDelayString = "${app.ticketing.jobs.reconcile-interval-ms:30000}")
    public void run() {
        Instant cutoff = Instant.now().minus(props.reconcileAfter());
        for (UUID id : payments.findIdsByStatusUpdatedBefore(PaymentStatus.IN_FLIGHT, cutoff, PageRequest.of(0, props.jobBatchSize()))) {
            try {
                reconcile(id);
            } catch (RuntimeException e) {
                log.error("Reconciliation failed for payment {}", id, e);
            }
        }
    }

    private void reconcile(UUID id) {
        Payment p = payments.findById(id).orElse(null);
        if (p == null || !PaymentStatus.IN_FLIGHT.contains(p.getStatus())) {
            return;
        }
        if (p.getProviderRef() == null) {
            if (p.getUpdatedAt().isBefore(Instant.now().minus(props.unackedPaymentTimeout()))) {
                settlement.settle(id, null, PaymentOutcome.FAILED, "Payment provider did not acknowledge the charge");
            }
            return;
        }
        try {
            ChargeResult r = gateway.fetchStatus(p.getProviderRef());
            if (r.outcome() != PaymentOutcome.PENDING) {
                settlement.settle(id, p.getProviderRef(), r.outcome(), r.failureReason());
            }
        } catch (PaymentGatewayException e) {
            log.warn("Could not fetch status of payment {}: {}", id, e.getMessage());
        }
    }
}
