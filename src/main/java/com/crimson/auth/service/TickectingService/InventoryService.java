package com.crimson.auth.service.TickectingService;

import com.crimson.auth.entity.ticket.ReservationItem;
import com.crimson.auth.exception.TicketingException;
import com.crimson.auth.repository.ticketrepository.ReservationItemRepository;
import com.crimson.auth.repository.ticketrepository.TicketTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class InventoryService {
    private final TicketTierRepository tiers;
    private final ReservationItemRepository items;

    public void take(List<ReservationItem> sortedItems) {
        for (ReservationItem it : sortedItems) {
            if (tiers.tryTake(it.getTierId(), it.getQuantity()) == 0) {
                throw TicketingException.conflict("Not enough tickets left for the requested tier");
            }
        }
    }

    public void release(UUID reservationId) {
        for (ReservationItem it : items.findByReservationIdOrderByTierAsc(reservationId)) {
            tiers.release(it.getTierId(), it.getQuantity());
        }
    }

    public boolean tryReacquire(UUID reservationId) {
        List<ReservationItem> taken = new ArrayList<>();
        for (ReservationItem it : items.findByReservationIdOrderByTierAsc(reservationId)) {
            if (tiers.tryTake(it.getTierId(), it.getQuantity()) == 1) {
                taken.add(it);
            } else {
                taken.forEach(t -> tiers.release(t.getTierId(), t.getQuantity()));
                return false;
            }
        }
        return true;
    }
}

