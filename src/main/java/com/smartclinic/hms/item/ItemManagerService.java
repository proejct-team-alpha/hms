package com.smartclinic.hms.item;

import com.smartclinic.hms.domain.Item;
import com.smartclinic.hms.domain.ItemCategory;
import com.smartclinic.hms.item.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemManagerService {

    private final ItemManagerRepository itemRepository;

    @Transactional(readOnly = true)
    public ItemDashboardDto getDashboard() {
        List<Item> all = itemRepository.findAllByOrderByNameAsc();
        List<Item> lowStock = itemRepository.findLowStockItems();

        return new ItemDashboardDto(
                all.size(),
                lowStock.size(),
                lowStock.stream().limit(5).map(ItemListDto::new).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<ItemListDto> getItemList(String category) {
        if (category == null || category.isBlank()) {
            return itemRepository.findAllByOrderByNameAsc().stream()
                    .map(ItemListDto::new).toList();
        }
        try {
            ItemCategory cat = ItemCategory.valueOf(category);
            return itemRepository.findByCategoryOrderByNameAsc(cat).stream()
                    .map(ItemListDto::new).toList();
        } catch (IllegalArgumentException e) {
            return itemRepository.findAllByOrderByNameAsc().stream()
                    .map(ItemListDto::new).toList();
        }
    }

    public List<ItemCategoryFilter> getCategoryFilters(String selected) {
        String sel = selected == null ? "" : selected;
        return List.of(
                new ItemCategoryFilter("전체", "", sel.isEmpty(), "/item-manager/item-list"),
                new ItemCategoryFilter("의료소모품", "MEDICAL_SUPPLIES",
                        "MEDICAL_SUPPLIES".equals(sel), "/item-manager/item-list?category=MEDICAL_SUPPLIES"),
                new ItemCategoryFilter("의료기기", "MEDICAL_EQUIPMENT",
                        "MEDICAL_EQUIPMENT".equals(sel), "/item-manager/item-list?category=MEDICAL_EQUIPMENT"),
                new ItemCategoryFilter("사무/비품", "GENERAL_SUPPLIES",
                        "GENERAL_SUPPLIES".equals(sel), "/item-manager/item-list?category=GENERAL_SUPPLIES")
        );
    }

    @Transactional(readOnly = true)
    public ItemFormDto getItemForm(Long id) {
        if (id == null) return new ItemFormDto();
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("물품을 찾을 수 없습니다."));
        return new ItemFormDto(item);
    }

    @Transactional
    public void saveItem(Long id, String name, String category, int quantity, int minQuantity) {
        ItemCategory cat = ItemCategory.valueOf(category);
        if (id == null) {
            itemRepository.save(Item.create(name, cat, quantity, minQuantity));
        } else {
            Item item = itemRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("물품을 찾을 수 없습니다."));
            item.update(name, cat, quantity, minQuantity);
        }
    }

    @Transactional
    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }
}
