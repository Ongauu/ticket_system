package com.crimson.auth.entity.ticket;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ticket_tiers")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;
    
    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;
}
