package com.crimson.auth.payment;

public record RefundRequest(String providerRef,
                            long amountCents,
                            String currency,
                            String idempotencyKey) {}
