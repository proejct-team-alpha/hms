package com.smartclinic.hms.admin.item;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.admin.item.dto.AdminItemListItemResponse;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Item;
import com.smartclinic.hms.domain.ItemCategory;
import com.smartclinic.hms.item.dto.ItemCategoryFilter;
import com.smartclinic.hms.item.dto.ItemFormDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminItemService {

    private final ItemRepository itemRepository;

    public List<AdminItemListItemResponse> getItemList(String category) {
        if (category == null || category.isBlank()) {
            return itemRepository.findAllByOrderByNameAsc().stream()
                    .map(AdminItemListItemResponse::new).collect(Collectors.toList());
        }
        try {
            ItemCategory cat = ItemCategory.valueOf(category);
            return itemRepository.findByCategoryOrderByNameAsc(cat).stream()
                    .map(AdminItemListItemResponse::new).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return itemRepository.findAllByOrderByNameAsc().stream()
                    .map(AdminItemListItemResponse::new).collect(Collectors.toList());
        }
    }

    public List<ItemCategoryFilter> getCategoryFilters(String selected) {
        String sel = selected == null ? "" : selected;
        return List.of(
                new ItemCategoryFilter("전체", "", sel.isEmpty(), "/admin/item/list"),
                new ItemCategoryFilter("의료소모품", "MEDICAL_SUPPLIES",
                        "MEDICAL_SUPPLIES".equals(sel), "/admin/item/list?category=MEDICAL_SUPPLIES"),
                new ItemCategoryFilter("의료기기", "MEDICAL_EQUIPMENT",
                        "MEDICAL_EQUIPMENT".equals(sel), "/admin/item/list?category=MEDICAL_EQUIPMENT"),
                new ItemCategoryFilter("사무/비품", "GENERAL_SUPPLIES",
                        "GENERAL_SUPPLIES".equals(sel), "/admin/item/list?category=GENERAL_SUPPLIES")
        );
    }

    public ItemFormDto getItemForm(Long id) {
        if (id == null) return new ItemFormDto();
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("물품을 찾을 수 없습니다. ID: " + id));
        return new ItemFormDto(item);
    }

    @Transactional
    public void saveItem(Long id, String name, String category, int quantity, int minQuantity) {
        ItemCategory cat = ItemCategory.valueOf(category);
        if (id == null) {
            itemRepository.save(Item.create(name, cat, quantity, minQuantity));
        } else {
            Item item = itemRepository.findById(id)
                    .orElseThrow(() -> CustomException.notFound("물품을 찾을 수 없습니다. ID: " + id));
            item.update(name, cat, quantity, minQuantity);
        }
    }

    @Transactional
    public void restockItem(Long id, int amount) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("물품을 찾을 수 없습니다. ID: " + id));
        item.addStock(amount);
    }

    @Transactional
    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }

    @Transactional
    public void createItem(String name, String category, int quantity, int minQuantity) {
        itemRepository.save(Item.create(name, ItemCategory.valueOf(category), quantity, minQuantity));
    }
}
