package com.groom.foocle.repository;

import com.groom.foocle.dto.res.MyStoreSimple;
import com.groom.foocle.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByIdAndUserId(Long id, Long userId);

    //필요한 정보만 조회 해봅시다~
    @Query("""
        select new com.groom.foocle.dto.res.MyStoreSimple(
            s.id, s.name, s.address
        )
        from Store s
        where s.user.id = :userId
        order by s.id desc
    """)
    List<MyStoreSimple> findSimpleByUserId(@Param("userId") Long userId);
}
