package com.crimson.auth.service.TickectingService;

import com.crimson.auth.config.TicketingProperties;
import com.crimson.auth.dto.request.CreateReservationRequest;
import com.crimson.auth.dto.response.ReservationResponse;
import com.crimson.auth.entity.ticket.*;
import com.crimson.auth.exception.TicketingException;
import com.crimson.auth.repository.ticketrepository.EventRepository;
import com.crimson.auth.repository.ticketrepository.ReservationItemRepository;
import com.crimson.auth.repository.ticketrepository.ReservationRepository;
import com.crimson.auth.repository.ticketrepository.TicketTierRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservations;
    private final ReservationItemRepository items;
    private final EventRepository events;
    private final TicketTierRepository tiers;
    private final InventoryService inventory;
    private final TransactionTemplate tx;
    private final TicketingProperties props;


    public ReservationResponse reserve(Long userId, String key, CreateReservationRequest req) {
        Map<Long, Integer> wanted = normalize(req);
        String hash = requestHash(req.eventId(), wanted);

        Optional<Reservation> existing = reservations.findByUserIdAndIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            return replay(existing.get(), hash);
        }
        try {
            return tx.execute(status -> create(userId, key, hash, req.eventId(), wanted));
        } catch (DataIntegrityViolationException e) {
            return reservations.findByUserIdAndIdempotencyKey(userId, key)
                    .map(r -> replay(r, hash))
                    .orElseThrow(() -> e);
        }
    }

    private ReservationResponse create(Long userId, String key, String hash, Long eventId, Map<Long, Integer> wanted) {
        Event event = events.findById(eventId).orElseThrow(() -> TicketingException.notFound("Event not found"));
        Instant now = Instant.now();
        if (event.getStatus() != EventStatus.PUBLISHED
                || now.isBefore(event.getSalesOpenAt()) || !now.isBefore(event.getSalesCloseAt())) {
            throw TicketingException.conflict("Ticket sales are not open for this event");
        }

        List<TicketTier> found = tiers.findAllById(wanted.keySet()).stream()
                .sorted(Comparator.comparing(TicketTier::getId)).toList();
        if (found.size() != wanted.size()) {
            throw TicketingException.notFound("Unknown ticket tier");
        }

        long total = 0;
        for (TicketTier t : found) {
            if (!t.getEventId().equals(eventId)) {
                throw TicketingException.badRequest("Tier " + t.getId() + " does not belong to this event");
            }
            int qty = wanted.get(t.getId());
            if (t.getAvailableQuantity() < qty) {
                throw TicketingException.conflict("Not enough tickets left for tier '" + t.getName() + "'");
            }
            total = Math.addExact(total, Math.multiplyExact(t.getPriceCents(), (long) qty));
        }

        Reservation r = reservations.saveAndFlush(Reservation.builder()
                .userId(userId)
                .eventId(eventId)
                .status(ReservationStatus.HELD)
                .totalCents(total)
                .currency(event.getCurrency())
                .expiresAt(now.plus(props.holdDuration()))
                .idempotencyKey(key)
                .requestHash(hash)
                .build());

        List<ReservationItem> its = found.stream()
                .map(t -> ReservationItem.builder()
                        .reservationId(r.getId())
                        .tierId(t.getId())
                        .quantity(wanted.get(t.getId()))
                        .unitPriceCents(t.getPriceCents())
                        .build())
                .toList();
        items.saveAllAndFlush(its);
        inventory.take(its);

        return ReservationResponse.of(r, its);
    }

    private ReservationResponse replay(Reservation r, String hash) {
        if (!r.getRequestHash().equals(hash)) {
            throw TicketingException.unprocessable("Idempotency-Key was already used with a different request");
        }
        return ReservationResponse.of(r, items.findByReservationIdOrderByTierAsc(r.getId()));
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(Long userId, UUID id) {
        Reservation r = reservations.findById(id)
                .filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> TicketingException.notFound("Reservation not found"));
        return ReservationResponse.of(r, items.findByReservationIdOrderByTierAsc(id));
    }

    @Transactional
    public void cancel(Long userId, UUID id) {
        Reservation r = reservations.findByIdForUpdate(id)
                .filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> TicketingException.notFound("Reservation not found"));
        if (r.getStatus() != ReservationStatus.HELD) {
            throw TicketingException.conflict("Only a HELD reservation can be cancelled (status: " + r.getStatus() + ")");
        }
        inventory.release(r.getId());
        r.setStatus(ReservationStatus.CANCELLED);
    }

    public boolean expireIfDue(UUID id) {
        return Boolean.TRUE.equals(tx.execute(status -> doExpire(id)));
    }

    private boolean doExpire(UUID id) {
        Optional<Reservation> locked = reservations.findByIdForUpdateSkipLocked(id);
        if (locked.isEmpty()) {
            return false;
        }
        Reservation r = locked.get();
        Instant now = Instant.now();
        boolean due = (r.getStatus() == ReservationStatus.HELD && r.getExpiresAt().isBefore(now))
                || (r.getStatus() == ReservationStatus.PAYMENT_PENDING
                && r.getExpiresAt().plus(props.paymentExpiryGrace()).isBefore(now));
        if (!due) {
            return false;
        }
        inventory.release(r.getId());
        r.setStatus(ReservationStatus.EXPIRED);
        log.info("Reservation {} expired, stock released", r.getId());
        return true;
    }

    public int expireDueBatch() {
        Instant now = Instant.now();
        List<UUID> ids = reservations.findExpiredCandidates(ReservationStatus.HELD, ReservationStatus.PAYMENT_PENDING,
                now, now.minus(props.paymentExpiryGrace()), PageRequest.of(0, props.jobBatchSize()));
        int expired = 0;
        for (UUID id : ids) {
            try {
                if (expireIfDue(id)) {
                    expired++;
                }
            } catch (RuntimeException e) {
                log.error("Failed to expire reservation {}", id, e);
            }
        }
        return expired;
    }

    private Map<Long, Integer> normalize(CreateReservationRequest req) {
        Map<Long, Integer> wanted = new TreeMap<>();
        int total = 0;
        for (CreateReservationRequest.Item i : req.items()) {
            wanted.merge(i.tierId(), i.quantity(), Integer::sum);
            total += i.quantity();
        }
        if (total > props.maxTicketsPerReservation()) {
            throw TicketingException.badRequest("At most " + props.maxTicketsPerReservation() + " tickets per reservation");
        }
        return wanted;
    }

    private static String requestHash(Long eventId, Map<Long, Integer> wanted) {
        String canonical = eventId + "|" + wanted.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(","));
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

