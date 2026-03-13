package com.smartclinic.hms.item.dto;

import com.smartclinic.hms.domain.Item;
import com.smartclinic.hms.domain.ItemCategory;
import lombok.Getter;

@Getter
public class ItemListDto {

    private final Long id;
    private final String name;
    private final String categoryText;
    private final int quantity;
    private final int minQuantity;
    private final boolean lowStock;

    public ItemListDto(Item item) {
        this.id = item.getId();
        this.name = item.getName();
        this.categoryText = toKorean(item.getCategory());
        this.quantity = item.getQuantity();
        this.minQuantity = item.getMinQuantity();
        this.lowStock = item.getQuantity() < item.getMinQuantity();
    }

    public static String toKorean(ItemCategory cat) {
        return switch (cat) {
            case MEDICAL_SUPPLIES -> "의료소모품";
            case MEDICAL_EQUIPMENT -> "의료기기";
            case GENERAL_SUPPLIES -> "사무/비품";
        };
    }
}
