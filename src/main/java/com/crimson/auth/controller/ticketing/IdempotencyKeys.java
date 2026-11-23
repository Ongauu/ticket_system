package com.crimson.auth.controller.ticketing;

import com.crimson.auth.exception.TicketingException;

import java.util.regex.Pattern;

final class IdempotencyKeys {
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9_.:-]{8,100}$");
    private IdempotencyKeys(){}
    static String require(String key){
        if (key == null || !VALID.matcher(key).matches()) {
            throw TicketingException.badRequest(
                    "Idempotency-Key header must be 8-100 chars of letters, digits, '_', '-', '.', ':' (use a UUID)");
        }
        return key;
    }
}
