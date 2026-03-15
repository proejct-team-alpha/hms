package com.smartclinic.hms.admin.item;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.domain.Item;
import com.smartclinic.hms.domain.ItemCategory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminItemService {

    private final ItemRepository itemRepository;

    public List<AdminItemDto> getItemList() {
        return itemRepository.findAllByOrderByNameAsc()
                .stream()
                .map(AdminItemDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createItem(String name, String category, int quantity, int minQuantity) {
        itemRepository.save(Item.create(name, ItemCategory.valueOf(category), quantity, minQuantity));
    }
}
