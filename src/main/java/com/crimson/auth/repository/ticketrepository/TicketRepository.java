package com.crimson.auth.repository.ticketrepository;

import com.crimson.auth.entity.ticket.Ticket;
import com.crimson.auth.entity.ticket.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    boolean existsByReservationId(UUID reservationId);

    List<Ticket> findByUserIdOrderByIssuedAtDesc(Long userId);

    List<Ticket> findByReservationId(UUID reservationId);

    Optional<Ticket> findByCode(String code);

    @Modifying
    @Query("UPDATE Ticket t SET t.status = :to, t.checkedInAt = :now " +
            "WHERE t.code = :code AND t.eventId = :eventId AND t.status = :from")
    int markCheckedIn(@Param("code") String code, @Param("eventId") Long eventId, @Param("now") Instant now,
                      @Param("from") TicketStatus from, @Param("to") TicketStatus to);

}
