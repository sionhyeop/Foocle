package com.groom.foocle.repository;

import com.groom.foocle.entity.StoreCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreCategoryRepository extends JpaRepository<StoreCategory, Long> {
    List<StoreCategory> findByIdIn(List<Long> ids);
}

