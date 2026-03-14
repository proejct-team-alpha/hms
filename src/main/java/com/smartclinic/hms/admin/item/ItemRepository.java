package com.smartclinic.hms.admin.item;

import com.smartclinic.hms.domain.Item;
import com.smartclinic.hms.domain.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByOrderByNameAsc();

    @Query("""
            select count(i.id)
            from Item i
            where i.quantity < i.minQuantity
            """)
    long countLowStockItems();

    @Query("""
            select i.category as category, count(i.id) as totalCount
            from Item i
            group by i.category
            """)
    List<CategoryCountProjection> findCategoryCounts();

    interface CategoryCountProjection {
        ItemCategory getCategory();

        Long getTotalCount();
    }
}
