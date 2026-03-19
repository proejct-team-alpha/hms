package com.smartclinic.hms.item.log;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemUsageLogRepository extends JpaRepository<ItemUsageLog, Long> {

    List<ItemUsageLog> findByReservationIdOrderByUsedAtAsc(Long reservationId);

    List<ItemUsageLog> findByReservationIdIsNullAndUsedAtBetweenOrderByUsedAtDesc(
            LocalDateTime start, LocalDateTime end);

    List<ItemUsageLog> findByReservationIdIsNullAndUsedByAndUsedAtBetweenOrderByUsedAtDesc(
            String usedBy, LocalDateTime start, LocalDateTime end);
}
