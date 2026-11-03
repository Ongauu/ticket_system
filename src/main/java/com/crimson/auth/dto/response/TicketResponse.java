package com.crimson.auth.dto.response;

import com.crimson.auth.entity.ticket.TicketStatus;

import java.util.UUID;

public record TicketResponse(UUID id, Long eventId, Long tierId, int seq, TicketStatus status, String qrPayload) {
}
