package com.crimson.auth.dto.response;

import com.crimson.auth.entity.ticket.Event;
import com.crimson.auth.entity.ticket.EventStatus;
import com.crimson.auth.entity.ticket.TicketTier;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record EventResponse(Long id, String name, String description, String venue, String currency, Instant startAt, Instant salesOpenAt, Instant salesCloseAt, EventStatus status, List<TierResponse> tiers) {
    public record TierResponse(Long id, String name, long priceCents, int available) {
    }

    public static EventResponse of(Event e, List<TicketTier> tiers) {
        List<TierResponse> t = tiers.stream()
                .sorted(Comparator.comparing(TicketTier::getId))
                .map(x -> new TierResponse(x.getId(), x.getName(), x.getPriceCents(), x.getAvailableQuantity()))
                .toList();
        return new EventResponse(e.getId(), e.getName(), e.getDescription(), e.getVenue(), e.getCurrency(),
                e.getStartsAt(), e.getSalesOpenAt(), e.getSalesCloseAt(), e.getStatus(), t);
    }
}
