package com.groom.foocle.repository;

import com.groom.foocle.entity.StoreImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreImageRepository extends JpaRepository<StoreImage, Long> {
    List<StoreImage> findAllByStoreIdOrderByIdDesc(Long storeId);
    List<StoreImage> findAllByStore_IdAndUuidIn(Long storeId, List<String> uuids);
    List<StoreImage> findAllByStore_IdAndIdIn(Long storeId, List<Long> ids);
}
