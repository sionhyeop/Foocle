package com.groom.foocle.dto.res;

import java.util.List;

public class StoreDtoRes {

    // 식별자 반환용
    public record IdRes(Long storeId) {}

    // 가게 상세 조회용
    public record Detail(
            Long id,
            String name,
            String address,
            String openTime,   // ex) "09:00"
            String closeTime,  // ex) "22:00"
            List<ImageItem>images,
            List<ShortsItem> shorts
    ) {
        // 상세조회에 포함될 이미지 정보
        public record ImageItem(
                Long id,
                String uuid,
                String url,
                String type,
                String description
        ) {}

        // 상세조회에 포함될 쇼츠 정보
        public record ShortsItem(
                Long id,
                String uuid,
                String title,
                String status,
                String url,
                String promotionText
        ) {}
    }
}

