package com.crimson.auth.payment;

public record WebhookEvent(String eventId,
                           String providerRef,
                           String paymentId,
                           PaymentOutcome outcome,
                           String failureReason) {
}
