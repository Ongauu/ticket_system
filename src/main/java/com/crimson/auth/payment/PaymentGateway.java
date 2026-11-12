package com.crimson.auth.payment;

import com.crimson.auth.exception.TicketingException;

public interface PaymentGateway {
    String name();

    ChargeResult charge(ChargeRequest request);

    ChargeResult fetchStatus(String providerRef);

    String refund(RefundRequest request);

    WebhookEvent parseWebhook(String rawBody, String signature);
}
