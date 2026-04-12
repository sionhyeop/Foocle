package com.groom.foocle.service;

import com.groom.foocle.apiPayload.code.exception.GeneralException;
import com.groom.foocle.apiPayload.code.status.ErrorStatus;
import com.groom.foocle.dto.res.ShortsDtoRes;
import com.groom.foocle.entity.ShortsVideo;
import com.groom.foocle.repository.ShortsVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShortsQueryService {
    private final ShortsVideoRepository shortsVideoRepository;

    @Transactional(readOnly = true)
    public ShortsDtoRes.ShortsDetailRes getByUuid(String uuid) {
        ShortsVideo sv = shortsVideoRepository.findByUuid(uuid)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SHORTS_NOT_FOUND));
        return new ShortsDtoRes.ShortsDetailRes(
                sv.getUuid(),
                sv.getStatus().name(),
                sv.getUrl(),
                sv.getTitle(),
                sv.getCreatedAt().toString(),
                sv.getPromotionText()
        );
    }
}