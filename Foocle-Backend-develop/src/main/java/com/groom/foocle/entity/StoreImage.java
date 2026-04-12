package com.groom.foocle.entity;

import com.groom.foocle.entity.enums.StoreImageType;
import com.groom.foocle.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StoreImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String uuid;

    private String fileName;

    private String url;

    // 어떤 섹션 이미지 인가용 (외관/내부/조리/음식)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreImageType type; // EXTERIOR, INTERIOR, COOKING, FOOD

    @Column(length = 100)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @Setter(AccessLevel.PACKAGE)
    private Store store;
}
