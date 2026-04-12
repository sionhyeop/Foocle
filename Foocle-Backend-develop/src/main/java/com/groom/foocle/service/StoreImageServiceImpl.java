package com.groom.foocle.service;

import com.groom.foocle.apiPayload.code.exception.GeneralException;
import com.groom.foocle.apiPayload.code.status.ErrorStatus;
import com.groom.foocle.dto.req.StoreImageDtoReq;
import com.groom.foocle.dto.res.StoreDtoRes;
import com.groom.foocle.entity.Store;
import com.groom.foocle.entity.StoreImage;
import com.groom.foocle.repository.StoreImageRepository;
import com.groom.foocle.repository.StoreRepository;
import com.groom.foocle.global.util.AmazonS3Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreImageServiceImpl implements StoreImageService {

    private final StoreRepository storeRepository;
    private final StoreImageRepository storeImageRepository;
    private final AmazonS3Util s3Util;

    public List<StoreDtoRes.Detail.ImageItem> upload(Long userId, Long storeId, List<MultipartFile> images,StoreImageDtoReq.Upload req) {

        Store store = storeRepository.findByIdAndUserId(storeId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.STORE_NOT_FOUND));

        List<StoreDtoRes.Detail.ImageItem> results = new ArrayList<>();

        List<String> descriptions = req.descriptions();

        for (int i = 0; i < images.size(); i++) {
            MultipartFile file = images.get(i);
            String url = s3Util.uploadImage(file);

            StoreImage image = StoreImage.builder()
                    .uuid(UUID.randomUUID().toString())
                    .fileName(file.getOriginalFilename())
                    .url(url)
                    .type(req.type())
                    .description((descriptions != null && i < descriptions.size()) ? descriptions.get(i) : null)
                    .build();

            store.addImage(image);

            storeImageRepository.save(image);

            results.add(new StoreDtoRes.Detail.ImageItem(
                    image.getId(),
                    image.getUuid(),
                    image.getUrl(),
                    image.getType().name(),
                    image.getDescription()
            ));
        }

        return results;
    }
}
