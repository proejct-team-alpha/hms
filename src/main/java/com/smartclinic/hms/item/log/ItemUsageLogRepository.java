package com.smartclinic.hms.item.log;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemUsageLogRepository extends JpaRepository<ItemUsageLog, Long> {

    List<ItemUsageLog> findByReservationIdOrderByUsedAtAsc(Long reservationId);

    List<ItemUsageLog> findByReservationIdIsNullAndUsedAtBetweenOrderByUsedAtDesc(
            LocalDateTime start, LocalDateTime end);

    List<ItemUsageLog> findByReservationIdIsNullAndUsedByAndUsedAtBetweenOrderByUsedAtDesc(
            String usedBy, LocalDateTime start, LocalDateTime end);

    List<ItemUsageLog> findByReservationIdIsNullAndUsedAtGreaterThanEqualAndUsedAtLessThanOrderByUsedAtDesc(
            LocalDateTime start, LocalDateTime endExclusive);

    @Query("""
            SELECT COALESCE(SUM(l.amount), 0)
            FROM ItemUsageLog l
            WHERE l.reservationId IS NULL
              AND l.usedAt >= :start
              AND l.usedAt < :endExclusive
            """)
    long sumAmountByReservationIdIsNullAndUsedAtRange(@Param("start") LocalDateTime start,
                                                      @Param("endExclusive") LocalDateTime endExclusive);
}
