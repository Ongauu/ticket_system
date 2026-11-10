package com.crimson.auth.service.TickectingService;

import com.crimson.auth.dto.request.CreateEventRequest;
import com.crimson.auth.dto.response.EventResponse;
import com.crimson.auth.dto.response.PageResponse;
import com.crimson.auth.entity.ticket.Event;
import com.crimson.auth.entity.ticket.EventStatus;
import com.crimson.auth.entity.ticket.TicketTier;
import com.crimson.auth.exception.TicketingException;
import com.crimson.auth.repository.ticketrepository.EventRepository;
import com.crimson.auth.repository.ticketrepository.TicketTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository events;
    private final TicketTierRepository tiers;

    @Transactional
    public EventResponse create(CreateEventRequest req){
        if(!req.salesCloseAt().isAfter(req.salesCloseAt())){
            throw TicketingException.badRequest("salesCloseAt must be after salesOpenAt");
        }

        Event event = events.save(Event.builder()
                .name(req.name())
                .description(req.description())
                .venue(req.venue())
                .currency(req.currency() == null ? "KES" : req.currency())
                .startsAt(req.startsAt())
                .salesOpenAt(req.salesOpenAt())
                .salesCloseAt(req.salesCloseAt())
                .build());

        List<TicketTier> created = tiers.saveAll(req.tiers().stream()
                .map(t -> TicketTier.builder()
                        .eventId(event.getId())
                        .name(t.name())
                        .priceCents(t.priceCents())
                        .totalQuantity(t.quantity())
                        .availableQuantity(t.quantity())
                        .build())
                .toList());
        return EventResponse.of(event, created);
    }

    @Transactional(readOnly = true)
    public PageResponse<EventResponse> listPublished(Pageable pageable){
        Page<Event> page = events.findByStatus(EventStatus.PUBLISHED, pageable);
        Map<Long, List<TicketTier>> byEvent = tiers.findByEventIdIn(page.getContent().stream().map(Event::getId).toList()).stream().collect(Collectors.groupingBy(TicketTier::getEventId));
        List<EventResponse> items = page.getContent().stream()
                .map(e -> EventResponse.of(e, byEvent.getOrDefault(e.getId(), List.of())))
                .toList();
        return new PageResponse<>(items, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EventResponse getPublished(Long id) {
        Event event = events.findById(id)
                .filter(e -> e.getStatus() == EventStatus.PUBLISHED)
                .orElseThrow(() -> TicketingException.notFound("Event not found"));
        return EventResponse.of(event, tiers.findByEventIdOrderByIdAsc(id));
    }
}
