package com.groom.foocle.service;

import com.groom.foocle.apiPayload.code.exception.GeneralException;
import com.groom.foocle.apiPayload.code.status.ErrorStatus;
import com.groom.foocle.converter.StoreConverter;
import com.groom.foocle.dto.req.StoreDtoReq;
import com.groom.foocle.dto.res.MyStoreSimple;
import com.groom.foocle.dto.res.StoreDtoRes;
import com.groom.foocle.entity.Store;
import com.groom.foocle.entity.StoreCategory;
import com.groom.foocle.entity.User;
import com.groom.foocle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final StoreCategoryRepository storeCategoryRepository;
    private final UserRepository userRepository;
    private final StoreImageRepository storeImageRepository;
    private final ShortsVideoRepository shortsVideoRepository;
    private final StoreConverter storeConverter;


    @Override
    public StoreDtoRes.IdRes create(Long userId, StoreDtoReq.Create req) {
        // 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 카테고리 확인
        List<StoreCategory> categories = storeCategoryRepository.findByIdIn(req.categoryIds());
        if (categories.size() != req.categoryIds().size()) {
            throw new GeneralException(ErrorStatus.CATEGORY_NOT_FOUND);
        }

        Store store = Store.builder()
                .name(req.name())
                .address(req.address())
                .openTime(req.openTime())
                .closeTime(req.closeTime())
                .user(user)
                .build();

        store.setCategories(categories);

        Store saved = storeRepository.save(store);
        return new StoreDtoRes.IdRes(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyStoreSimple> getMyStoresSimple(Long userId) {
        return storeRepository.findSimpleByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreDtoRes.Detail getDetail(Long userId, Long storeId) {
        Store store = storeRepository.findByIdAndUserId(storeId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.STORE_NOT_FOUND));

        var imageEntities = storeImageRepository.findAllByStoreIdOrderByIdDesc(store.getId());
        var images = imageEntities.stream()
                .map(storeConverter::toImageItem)
                .toList();

        var shortsEntities = shortsVideoRepository.findAllByStoreIdOrderByIdDesc(store.getId());
        var shorts = shortsEntities.stream()
                .map(storeConverter::toShortsItem)
                .toList();

        return new StoreDtoRes.Detail(
                store.getId(),
                store.getName(),
                store.getAddress(),
                store.getOpenTime().toString(),
                store.getCloseTime().toString(),
                images,
                shorts
        );
    }

}
