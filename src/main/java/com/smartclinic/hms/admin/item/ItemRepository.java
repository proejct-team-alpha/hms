package com.smartclinic.hms.admin.item;

import com.smartclinic.hms.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<StockLevelProjection> findAllProjectedBy();

    interface StockLevelProjection {
        int getQuantity();

        int getMinQuantity();
    }
}
