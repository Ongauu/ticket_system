package com.crimson.auth.dto.response;

import com.crimson.auth.entity.ticket.Reservation;
import com.crimson.auth.entity.ticket.ReservationItem;
import com.crimson.auth.entity.ticket.ReservationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(UUID id, Long eventId, ReservationStatus status, long totalCents, String currency,
                                  Instant expiresAt, Instant createdAt, List<Item> items) {
    public record Item(Long tierId, int quantity, long unitPriceCents) {
    }

    public static ReservationResponse of(Reservation r, List<ReservationItem> items) {
        return new ReservationResponse(r.getId(), r.getEventId(), r.getStatus(), r.getTotalCents(), r.getCurrency(),
                r.getExpiresAt(), r.getCreatedAt(),
                items.stream().map(i -> new Item(i.getTierId(), i.getQuantity(), i.getUnitPriceCents())).toList());
    }
}
