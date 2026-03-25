package com.smartclinic.hms.item.log;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemStockLogRepository extends JpaRepository<ItemStockLog, Long> {
    List<ItemStockLog> findAllByOrderByCreatedAtDesc();

    List<ItemStockLog> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime endExclusive);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM ItemStockLog l WHERE l.type = :type")
    long sumAmountByType(@Param("type") ItemStockType type);

    @Query("""
            SELECT COALESCE(SUM(l.amount), 0)
            FROM ItemStockLog l
            WHERE l.type = :type
              AND l.createdAt >= :start
              AND l.createdAt < :endExclusive
            """)
    long sumAmountByTypeAndCreatedAtRange(@Param("type") ItemStockType type,
                                          @Param("start") LocalDateTime start,
                                          @Param("endExclusive") LocalDateTime endExclusive);

    List<ItemStockLog> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime start, LocalDateTime end);
}
