package com.crimson.auth.entity.ticket;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 200)
    private String venue;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "sales_open_at", nullable = false)
    private Instant salesOpenAt;

    @Column(name = "sales_close_at", nullable = false)
    private Instant salesCloseAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

}
