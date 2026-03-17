package com.smartclinic.hms.item.log;

import java.time.format.DateTimeFormatter;

public class ItemStockLogDto {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String itemName;
    private final String typeText;
    private final boolean isIn;
    private final int amount;
    private final String createdAt;

    public ItemStockLogDto(ItemStockLog log) {
        this.itemName = log.getItemName();
        this.isIn = log.getType() == ItemStockType.IN;
        this.typeText = this.isIn ? "입고" : "출고";
        this.amount = log.getAmount();
        this.createdAt = log.getCreatedAt().format(FMT);
    }

    public String getItemName() { return itemName; }
    public String getTypeText() { return typeText; }
    public boolean isIn() { return isIn; }
    public int getAmount() { return amount; }
    public String getCreatedAt() { return createdAt; }
}
