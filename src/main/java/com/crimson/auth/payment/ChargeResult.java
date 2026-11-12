package com.crimson.auth.payment;

public record ChargeResult(String providerRef,
                           PaymentOutcome outcome,
                           String failureReason) {
}
