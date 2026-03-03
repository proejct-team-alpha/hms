package com.smartclinic.hms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 물품 엔티티 (ERD §2.7)
 */
@Entity
@Table(name = "item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ItemCategory category;

    @Column(nullable = false)
    private int quantity = 0;

    @Column(name = "min_quantity", nullable = false)
    private int minQuantity = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static Item create(String name, ItemCategory category, int quantity, int minQuantity) {
        Item i = new Item();
        i.name = name;
        i.category = category;
        i.quantity = quantity;
        i.minQuantity = minQuantity;
        return i;
    }

    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void update(String name, ItemCategory category, int quantity, int minQuantity) {
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.minQuantity = minQuantity;
    }
}
