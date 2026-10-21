package com.crimson.auth.repository.ticketrepository;

import com.crimson.auth.entity.ticket.TicketTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TicketTierRepository extends JpaRepository<TicketTier, Long> {
    List<TicketTier> findByEventIdOrderByIdAsc(Long eventId);

    List<TicketTier> findByEventIdIn(Collection<Long> eventIds);


    @Modifying
    @Query("UPDATE TicketTier t SET t.availableQuantity = t.availableQuantity - :qty " +
            "WHERE t.id = :id AND t.availableQuantity >= :qty")
    int tryTake(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE TicketTier t SET t.availableQuantity = t.availableQuantity + :qty WHERE t.id = :id")
    int release(@Param("id") Long id, @Param("qty") int qty);

}
