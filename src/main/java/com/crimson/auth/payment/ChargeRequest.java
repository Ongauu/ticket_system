package com.crimson.auth.payment;

import java.util.UUID;

public record ChargeRequest (UUID paymentId,
                             Long userId,
                             long amountCents,
                             String currency,
                             String description,
                             String idempotencyKey,
                             String paymentToken){}

