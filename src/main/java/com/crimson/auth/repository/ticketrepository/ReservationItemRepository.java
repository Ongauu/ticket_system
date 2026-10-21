package com.crimson.auth.repository.ticketrepository;

import com.crimson.auth.entity.ticket.ReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationItemRepository extends JpaRepository<ReservationItem, Long> {
    List<ReservationItem> findByReservationIdOrderByTierAsc(UUID reservationId);
}
