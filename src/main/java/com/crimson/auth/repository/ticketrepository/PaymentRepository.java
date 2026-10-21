package com.crimson.auth.repository.ticketrepository;

import com.crimson.auth.entity.ticket.Payment;
import com.crimson.auth.entity.ticket.PaymentStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    Optional<Payment> findByProviderAndProviderRef(String provider, String providerRef);

    boolean existsByReservationIdAndStatusIn(UUID reservationId, Collection<PaymentStatus> statuses);

    @Query("select p.reservationId from Payment p where p.id = :id")
    Optional<UUID> findReservationId(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdateSkipLocked(@Param("id") UUID id);

    @Query("select p.id from Payment p where p.status in :statuses and p.updatedAt < :cutoff order by p.updatedAt")
    List<UUID> findIdsByStatusUpdatedBefore(@Param("statuses") Collection<PaymentStatus> statuses,
                                            @Param("cutoff") Instant cutoff,
                                            Pageable pageable);

    @Modifying
    @Query(value = "INSERT INTO payment_webhook_events (provider, event_id) VALUES (:provider, :eventId) " +
            "ON CONFLICT DO NOTHING", nativeQuery = true)
    int recordWebhookEvent(@Param("provider") String provider, @Param("eventId") String eventId);
}
