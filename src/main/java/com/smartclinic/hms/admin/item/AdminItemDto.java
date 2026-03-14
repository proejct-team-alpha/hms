package com.smartclinic.hms.admin.item;

import com.smartclinic.hms.domain.Item;
import com.smartclinic.hms.domain.ItemCategory;
import lombok.Getter;

@Getter
public class AdminItemDto {

    private final Long id;
    private final String name;
    private final String categoryText;
    private final int quantity;
    private final int minQuantity;
    private final boolean lowStock;
    private final String stockBadgeClass;

    public AdminItemDto(Item item) {
        this.id = item.getId();
        this.name = item.getName();
        this.categoryText = toCategoryText(item.getCategory());
        this.quantity = item.getQuantity();
        this.minQuantity = item.getMinQuantity();
        this.lowStock = item.getQuantity() < item.getMinQuantity();
        this.stockBadgeClass = this.lowStock
                ? "px-2 py-1 text-xs font-medium rounded-full bg-red-100 text-red-700"
                : "px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-700";
    }

    private String toCategoryText(ItemCategory category) {
        return switch (category) {
            case MEDICAL_SUPPLIES -> "의료 소모품";
            case MEDICAL_EQUIPMENT -> "의료 장비";
            case GENERAL_SUPPLIES -> "일반 소모품";
        };
    }
}
