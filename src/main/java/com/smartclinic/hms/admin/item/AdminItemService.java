package com.smartclinic.hms.admin.item;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.admin.item.dto.AdminItemListItemResponse;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Item;
import com.smartclinic.hms.domain.ItemCategory;
import com.smartclinic.hms.item.ItemManagerService;
import com.smartclinic.hms.item.dto.ItemCategoryFilter;
import com.smartclinic.hms.item.dto.ItemFormDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminItemService {

    private static final char[] CHOSUNG = {'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'};

    private final ItemRepository itemRepository;
    private final ItemManagerService itemManagerService;

    public List<AdminItemListItemResponse> getItemList(String category, String keyword) {
        return getItemsByCategory(category).stream()
                .filter(item -> matchesKeyword(item.getName(), keyword))
                .map(AdminItemListItemResponse::new)
                .collect(Collectors.toList());
    }

    private List<Item> getItemsByCategory(String category) {
        if (category == null || category.isBlank()) {
            return itemRepository.findAllByOrderByNameAsc();
        }
        try {
            ItemCategory cat = ItemCategory.valueOf(category);
            return itemRepository.findByCategoryOrderByNameAsc(cat);
        } catch (IllegalArgumentException e) {
            return itemRepository.findAllByOrderByNameAsc();
        }
    }

    private boolean matchesKeyword(String itemName, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        String normalizedItemName = itemName.toLowerCase(Locale.ROOT);
        if (normalizedItemName.contains(normalizedKeyword)) {
            return true;
        }

        if (isChosungOnly(normalizedKeyword)) {
            return extractChosung(itemName).contains(normalizedKeyword);
        }

        return false;
    }

    private boolean isChosungOnly(String keyword) {
        return keyword.chars().allMatch(this::isChosungCharacter);
    }

    private boolean isChosungCharacter(int character) {
        return character >= 'ㄱ' && character <= 'ㅎ';
    }

    private String extractChosung(String value) {
        StringBuilder builder = new StringBuilder();
        for (char character : value.toCharArray()) {
            if (character >= 0xAC00 && character <= 0xD7A3) {
                builder.append(CHOSUNG[(character - 0xAC00) / 588]);
                continue;
            }
            builder.append(Character.toLowerCase(character));
        }
        return builder.toString();
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
        itemManagerService.restockItem(id, amount);
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
