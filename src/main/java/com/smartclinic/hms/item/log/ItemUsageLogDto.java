package com.smartclinic.hms.item.log;

import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class ItemUsageLogDto {

    private final String itemName;
    private final int amount;
    private final String usedAt;

    public ItemUsageLogDto(ItemUsageLog log) {
        this.itemName = log.getItemName();
        this.amount = log.getAmount();
        this.usedAt = log.getUsedAt().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
