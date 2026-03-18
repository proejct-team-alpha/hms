package com.smartclinic.hms.item;

import com.smartclinic.hms.domain.Item;
import com.smartclinic.hms.domain.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ItemManagerRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByOrderByNameAsc();

    List<Item> findByCategoryOrderByNameAsc(ItemCategory category);

    @Query("SELECT i FROM Item i WHERE i.quantity < i.minQuantity ORDER BY (i.minQuantity - i.quantity) DESC")
    List<Item> findLowStockItems();

}
