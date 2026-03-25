package com.smartclinic.hms.item.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ItemDashboardDto {
    private final long totalItems;
    private final long lowStockCount;
    private final List<ItemListDto> lowStockItems;
    private final List<ItemChartDayDto> chartDays;

    public boolean isAllNormal() {
        return lowStockCount == 0;
    }
}
