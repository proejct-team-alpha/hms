package com.smartclinic.hms.item.dto;

import com.smartclinic.hms.domain.Item;
import lombok.Getter;

@Getter
public class ItemFormDto {

    private final Long id;
    private final String name;
    private final String category;
    private final int quantity;
    private final int minQuantity;
    private final boolean edit;
    private final boolean medicalSupplies;
    private final boolean medicalEquipment;
    private final boolean generalSupplies;

    // 신규 등록용
    public ItemFormDto() {
        this.id = null;
        this.name = "";
        this.category = "MEDICAL_SUPPLIES";
        this.quantity = 0;
        this.minQuantity = 0;
        this.edit = false;
        this.medicalSupplies = true;
        this.medicalEquipment = false;
        this.generalSupplies = false;
    }

    // 수정용
    public ItemFormDto(Item item) {
        this.id = item.getId();
        this.name = item.getName();
        this.category = item.getCategory().name();
        this.quantity = item.getQuantity();
        this.minQuantity = item.getMinQuantity();
        this.edit = true;
        this.medicalSupplies = item.getCategory().name().equals("MEDICAL_SUPPLIES");
        this.medicalEquipment = item.getCategory().name().equals("MEDICAL_EQUIPMENT");
        this.generalSupplies = item.getCategory().name().equals("GENERAL_SUPPLIES");
    }
}
