package com.crimson.auth.job;

import com.crimson.auth.service.TickectingService.ReservationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ticketing.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class ReservationExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);
    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${app.ticketing.jobs.expiry-interval-ms:10000}")
    public void run() {
        int n = reservationService.expireDueBatch();
        if (n > 0) {
            log.info("Expired {} reservations", n);
        }
    }
}
