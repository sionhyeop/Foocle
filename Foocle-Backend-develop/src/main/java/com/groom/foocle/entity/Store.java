package com.groom.foocle.entity;

import com.groom.foocle.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Store extends BaseEntity {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String address;

    private LocalTime openTime;

    private LocalTime closeTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoreImage> storeImages = new ArrayList<>();

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoreCategoryMap> storeCategoryMaps = new ArrayList<>();

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShortsVideo> shorts = new ArrayList<>();
    public void addImage(StoreImage img) {
        storeImages.add(img);
        img.setStore(this);
    }
    public void removeImage(StoreImage image) {
        storeImages.remove(image);
        image.setStore(null);
    }
    public void addShorts(ShortsVideo s) {
        shorts.add(s);
        s.setStore(this);
    }

    // 카테고리 매핑 편의 메서드
    public void setCategories(List<StoreCategory> categories) {
        if (this.storeCategoryMaps == null) {
            this.storeCategoryMaps = new ArrayList<>();
        } else {
            this.storeCategoryMaps.clear();
        }
        for (StoreCategory c : categories) {
            StoreCategoryMap map = new StoreCategoryMap();
            map.setStore(this);
            map.setStoreCategory(c);
            this.storeCategoryMaps.add(map);
        }
    }

}
