package com.crimson.auth.controller.ticketing;

import com.crimson.auth.exception.TicketingException;
import com.crimson.auth.payment.PaymentGateway;
import com.crimson.auth.payment.WebhookEvent;
import com.crimson.auth.service.TickectingService.PaymentSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class WebhookController {
    private final PaymentGateway gateway;
    private final PaymentSettlementService settlement;

    @PostMapping("/api/webhooks/payments/{provider}")
    public ResponseEntity<Void> receive(@PathVariable String provider,
                                        @RequestHeader(value = "X-Signature", required = false) String signature,
                                        @RequestBody String rawBody) {
        if (!gateway.name().equals(provider)) {
            throw TicketingException.notFound("Unknown payment provider");
        }
        WebhookEvent event = gateway.parseWebhook(rawBody, signature);
        settlement.handleWebhook(provider, event);
        return ResponseEntity.ok().build();
    }
}
