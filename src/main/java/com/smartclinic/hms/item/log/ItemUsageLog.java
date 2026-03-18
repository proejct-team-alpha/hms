package com.smartclinic.hms.item.log;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "item_usage_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = true)
    private Long reservationId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(nullable = false)
    private int amount;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @Column(name = "used_by", length = 100)
    private String usedBy;

    public static ItemUsageLog of(Long reservationId, Long itemId, String itemName, int amount) {
        ItemUsageLog log = new ItemUsageLog();
        log.reservationId = reservationId;
        log.itemId = itemId;
        log.itemName = itemName;
        log.amount = amount;
        log.usedAt = LocalDateTime.now();
        return log;
    }

    public static ItemUsageLog of(Long reservationId, Long itemId, String itemName, int amount, String usedBy) {
        ItemUsageLog log = of(reservationId, itemId, itemName, amount);
        log.usedBy = usedBy;
        return log;
    }
}
