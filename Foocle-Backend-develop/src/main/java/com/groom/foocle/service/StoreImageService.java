package com.groom.foocle.service;

import com.groom.foocle.dto.req.StoreImageDtoReq;
import com.groom.foocle.dto.res.StoreDtoRes;
import com.groom.foocle.entity.enums.StoreImageType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StoreImageService {
    List<StoreDtoRes.Detail.ImageItem> upload(Long userId, Long storeId, List<MultipartFile> images,StoreImageDtoReq.Upload req);
}
