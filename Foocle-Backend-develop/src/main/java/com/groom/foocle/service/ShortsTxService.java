package com.groom.foocle.service;

import com.groom.foocle.apiPayload.code.exception.GeneralException;
import com.groom.foocle.apiPayload.code.status.ErrorStatus;
import com.groom.foocle.entity.ShortsVideo;
import com.groom.foocle.repository.ShortsVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShortsTxService {

    private final ShortsVideoRepository shortsVideoRepository;

    @Transactional
    public ShortsVideo loadForUpdate(Long id) {
        return shortsVideoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SHORTS_NOT_FOUND));
    }

    @Transactional
    public void markProcessing(Long svId) {
        ShortsVideo sv = shortsVideoRepository.findByIdForUpdate(svId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SHORTS_NOT_FOUND));
        sv.markProcessing();
    }

    @Transactional
    public void markDoneWithUrl(Long svId, String url) {
        ShortsVideo sv = shortsVideoRepository.findByIdForUpdate(svId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SHORTS_NOT_FOUND));
        sv.markDoneWithUrl(url);
    }

    @Transactional
    public void markFailed(Long svId) {
        ShortsVideo sv = shortsVideoRepository.findByIdForUpdate(svId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SHORTS_NOT_FOUND));
        sv.markFailed();
    }

    @Transactional
    public void markDoneWithUrlAndPromo(Long svId, String url, String promo) {
        ShortsVideo sv = loadForUpdate(svId);
        sv.markDoneWithUrl(url);
        if (promo != null && !promo.isBlank()) {
            sv.updatePromotionText(promo);
        }
    }
}
