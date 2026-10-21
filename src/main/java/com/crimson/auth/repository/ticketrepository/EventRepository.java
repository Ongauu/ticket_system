package com.crimson.auth.repository.ticketrepository;

import com.crimson.auth.entity.ticket.Event;
import com.crimson.auth.entity.ticket.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findByStatus (EventStatus status, Pageable pageable);
}
