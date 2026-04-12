package com.groom.foocle.repository;

import com.groom.foocle.entity.ShortsVideo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShortsVideoRepository extends JpaRepository<ShortsVideo, Long> {

    Optional<ShortsVideo> findByUuid(String uuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShortsVideo s where s.id = :id")
    Optional<ShortsVideo> findByIdForUpdate(@Param("id") Long id);

    List<ShortsVideo> findAllByStoreIdOrderByIdDesc(Long storeId);
}
