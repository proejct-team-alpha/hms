package com.smartclinic.hms.item.log;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemStockLogRepository extends JpaRepository<ItemStockLog, Long> {
    List<ItemStockLog> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM ItemStockLog l WHERE l.type = :type")
    long sumAmountByType(@Param("type") ItemStockType type);

    List<ItemStockLog> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime start, LocalDateTime end);
}
