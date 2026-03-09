package com.smartclinic.hms.admin.item;

import com.smartclinic.hms.domain.Item;
import com.smartclinic.hms.domain.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<StockLevelProjection> findAllProjectedBy();

    @Query("""
            select i.category as category, count(i.id) as totalCount
            from Item i
            group by i.category
            """)
    List<CategoryCountProjection> findCategoryCounts();

    interface StockLevelProjection {
        int getQuantity();

        int getMinQuantity();
    }

    interface CategoryCountProjection {
        ItemCategory getCategory();

        Long getTotalCount();
    }
}
