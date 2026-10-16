package com.crimson.auth.entity.ticket;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity(name = "payment_webhook_events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentWebHookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
