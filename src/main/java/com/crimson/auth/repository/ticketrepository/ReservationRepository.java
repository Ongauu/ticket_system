package com.crimson.auth.repository.ticketrepository;

import com.crimson.auth.entity.ticket.Reservation;
import com.crimson.auth.entity.ticket.ReservationStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findByIdForUpdateSkipLocked(@Param("id") UUID id);

    @Query("select r.id from Reservation r where " +
            "(r.status = :held and r.expiresAt < :now) or (r.status = :pending and r.expiresAt < :graceCutoff) " +
            "order by r.expiresAt")
    List<UUID> findExpiredCandidates(@Param("held") ReservationStatus held,
                                     @Param("pending") ReservationStatus pending,
                                     @Param("now") Instant now,
                                     @Param("graceCutoff") Instant graceCutoff,
                                     Pageable pageable);

}
