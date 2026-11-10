package com.crimson.auth.service.TickectingService;

import com.crimson.auth.dto.request.CheckInRequest;
import com.crimson.auth.dto.response.TicketResponse;
import com.crimson.auth.entity.ticket.Reservation;
import com.crimson.auth.entity.ticket.ReservationItem;
import com.crimson.auth.entity.ticket.Ticket;
import com.crimson.auth.entity.ticket.TicketStatus;
import com.crimson.auth.exception.TicketingException;
import com.crimson.auth.repository.ticketrepository.ReservationItemRepository;
import com.crimson.auth.repository.ticketrepository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository tickets;
    private final ReservationItemRepository items;
    private final TicketCodeService codes;

    @Transactional(propagation = Propagation.MANDATORY)
    public void issueFor(Reservation r){
        if(tickets.existsByReservationId(r.getId())){
            return;
        }
        List<Ticket> out = new ArrayList<>();
        for (ReservationItem it : items.findByReservationIdOrderByTierAsc(r.getId())){
            for (int seq = 1; seq <= it.getQuantity(); seq++) {
                out.add(Ticket.builder()
                        .reservationId(r.getId())
                        .userId(r.getUserId())
                        .eventId(r.getEventId())
                        .tierId(it.getTierId())
                        .seq(seq)
                        .code(codes.newCode())
                        .status(TicketStatus.ISSUED)
                        .build());
            }
        }
        tickets.saveAll(out);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listForUser(Long userId){
        return tickets.findByUserIdOrderByIssuedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void checkIn(CheckInRequest req){
        String code = codes.verify(req.qrPayload()).orElseThrow(() -> TicketingException.badRequest("Invalid ticket"));
        int updated = tickets.markCheckedIn(code, req.eventId(), Instant.now(), TicketStatus.ISSUED, TicketStatus.CHECKED_IN);
        if(updated == 1){
            return;
        }
        Ticket t = tickets.findByCode(code).filter(x -> x.getEventId().equals(req.eventId()))
                .orElseThrow(() -> TicketingException.notFound("Ticket not found for this event"));
        throw TicketingException.conflict("Ticket is " + t.getStatus());
    }

    private TicketResponse toResponse(Ticket t) {
        return new TicketResponse(t.getId(), t.getEventId(), t.getTierId(), t.getSeq(), t.getStatus(),
                codes.qrPayload(t.getCode()));
    }
}
