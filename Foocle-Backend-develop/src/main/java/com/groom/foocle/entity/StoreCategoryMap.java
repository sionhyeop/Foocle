package com.groom.foocle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class StoreCategoryMap {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_category_id", nullable = false)
    private StoreCategory storeCategory;

    void setStore(Store store) { this.store = store; }
    void setStoreCategory(StoreCategory category) { this.storeCategory = category; }
}
