package com.groom.foocle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class StoreCategory {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=50)
    private String category;

    @OneToMany(mappedBy = "storeCategory", cascade = CascadeType.ALL)
    private List<StoreCategoryMap> storeCategoryMaps = new ArrayList<>();
}
