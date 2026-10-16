package com.crimson.auth.entity.ticket;

import java.util.EnumSet;
import java.util.Set;

public enum PaymentStatus {
    INITIATED, PENDING, SUCCEEDED, FAILED, REFUND_PENDING, REFUNDED;
    public static final Set<PaymentStatus> IN_FLIGHT = EnumSet.of(INITIATED, PENDING);
}
