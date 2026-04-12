package com.groom.foocle.converter;

import com.groom.foocle.dto.res.StoreDtoRes;
import com.groom.foocle.entity.ShortsVideo;
import com.groom.foocle.entity.StoreImage;
import org.springframework.stereotype.Component;

@Component
public class StoreConverter {

    public StoreDtoRes.Detail.ImageItem toImageItem(StoreImage img) {
        return new StoreDtoRes.Detail.ImageItem(
                img.getId(),
                img.getUuid(),
                img.getUrl(),
                img.getType().name(),
                img.getDescription()
        );
    }

    public StoreDtoRes.Detail.ShortsItem toShortsItem(ShortsVideo s) {
        return new StoreDtoRes.Detail.ShortsItem(
                s.getId(),
                s.getUuid(),
                s.getTitle(),
                s.getStatus().name(),
                s.getUrl(),
                s.getPromotionText()
        );
    }
}

