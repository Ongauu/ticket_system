package com.crimson.auth.entity.ticket;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "reservation_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ReservationItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "tier_id", nullable = false)
    private Long tierId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;
}
