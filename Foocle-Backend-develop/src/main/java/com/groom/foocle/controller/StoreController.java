package com.groom.foocle.controller;

import com.groom.foocle.apiPayload.ApiResponse;
import com.groom.foocle.common.security.JwtTokenProvider;
import com.groom.foocle.dto.req.StoreDtoReq;
import com.groom.foocle.dto.req.StoreImageDtoReq;
import com.groom.foocle.dto.res.MyStoreSimple;
import com.groom.foocle.dto.res.StoreDtoRes;
import com.groom.foocle.service.StoreImageService;
import com.groom.foocle.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;
    private final StoreImageService storeImageService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "가게 정보 저장", description = "가게 이름/주소/영업시간/카테고리로 가게를 생성합니다.")
    @PostMapping("")
    public ApiResponse<StoreDtoRes.IdRes> createStore(@RequestBody @Valid StoreDtoReq.Create req) {
        Long userId = jwtTokenProvider.getUserIdFromToken();
        StoreDtoRes.IdRes res = storeService.create(userId, req);
        return ApiResponse.onSuccess(res);
    }

    @Operation(summary = "내 가게 간단 목록", description = "로그인 사용자의 가게 목록을 (이름, 주소만) 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<List<MyStoreSimple>> getMyStores() {
        Long userId = jwtTokenProvider.getUserIdFromToken();
        List<MyStoreSimple> res = storeService.getMyStoresSimple(userId);
        return ApiResponse.onSuccess(res);
    }

    @Operation(summary = "가게 상세 조회", description = "가게 이름/주소/영업시간 + 저장된 이미지 + 생성된 쇼츠를 조회합니다.")
    @GetMapping("/{storeId}")
    public ApiResponse<StoreDtoRes.Detail> getStoreDetail(@PathVariable Long storeId) {
        Long userId = jwtTokenProvider.getUserIdFromToken();
        StoreDtoRes.Detail res = storeService.getDetail(userId, storeId);
        return ApiResponse.onSuccess(res);
    }

    @Operation(summary = "가게 이미지 업로드",
            description = "멀티파트로 이미지 파일을 업로드, type=EXTERIOR(외관), INTERIOR(내부), KITCHEN(주방), FOOD(음식), " +
                    "descriptions는 파일 순서와 동일하게 전달.")
    @PostMapping(value = "/{storeId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<StoreDtoRes.Detail.ImageItem>> uploadStoreImages(
            @PathVariable Long storeId,
            @RequestPart("files") List<MultipartFile> images,
            @RequestPart("meta") @Valid StoreImageDtoReq.Upload req
    ) {
        Long userId = jwtTokenProvider.getUserIdFromToken();
        var res = storeImageService.upload(userId, storeId,images ,req);
        return ApiResponse.onSuccess(res);
    }
}

