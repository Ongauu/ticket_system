package com.crimson.auth.job;

import com.crimson.auth.config.TicketingProperties;
import com.crimson.auth.entity.ticket.PaymentStatus;
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
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ticketing.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class RefundJob {
    private static final Logger log = LoggerFactory.getLogger(RefundJob.class);

    private final PaymentRepository payments;
    private final PaymentSettlementService settlement;
    private final TicketingProperties props;

    @Scheduled(fixedDelayString = "${app.ticketing.jobs.refund-interval-ms:15000}")
    public void run() {
        List<UUID> ids = payments.findIdsByStatusUpdatedBefore(List.of(PaymentStatus.REFUND_PENDING), Instant.now(),
                PageRequest.of(0, props.jobBatchSize()));
        for (UUID id : ids) {
            try {
                settlement.refund(id);
            } catch (RuntimeException e) {
                log.error("Refund worker failed for payment {}", id, e);
            }
        }
    }
}

