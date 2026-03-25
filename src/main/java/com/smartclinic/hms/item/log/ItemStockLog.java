package com.smartclinic.hms.item.log;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "item_stock_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemStockLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ItemStockType type;

    @Column(nullable = false)
    private int amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    public static ItemStockLog of(Long itemId, String itemName, ItemStockType type, int amount, String performedBy) {
        ItemStockLog log = new ItemStockLog();
        log.itemId = itemId;
        log.itemName = itemName;
        log.type = type;
        log.amount = amount;
        log.createdAt = LocalDateTime.now();
        log.performedBy = performedBy;
        return log;
    }
}
