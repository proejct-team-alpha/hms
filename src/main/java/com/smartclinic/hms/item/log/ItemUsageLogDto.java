package com.smartclinic.hms.item.log;

import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class ItemUsageLogDto {

    private final Long id;
    private final String itemName;
    private final int amount;
    private final String usedAt;
    private final String usedBy;

    public ItemUsageLogDto(ItemUsageLog log) {
        this.id = log.getId();
        this.itemName = log.getItemName();
        this.amount = log.getAmount();
        this.usedAt = log.getUsedAt().format(DateTimeFormatter.ofPattern("HH:mm"));
        this.usedBy = log.getUsedBy();
    }
}
