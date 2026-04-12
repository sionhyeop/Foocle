package com.groom.foocle.controller;

import com.groom.foocle.apiPayload.ApiResponse;
import com.groom.foocle.apiPayload.code.exception.GeneralException;
import com.groom.foocle.apiPayload.code.status.ErrorStatus;
import com.groom.foocle.dto.req.ShortsDtoReq;
import com.groom.foocle.dto.res.ShortsDtoRes;
import com.groom.foocle.entity.ShortsVideo;
import com.groom.foocle.service.ShortsCommandService;
import com.groom.foocle.service.ShortsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ShortsController {

    private final ShortsCommandService shortsCommandService;
    private final ShortsQueryService shortsQueryService;

    @Operation(
            summary = "쇼츠 생성 요청(비동기)",
            description = """
            이미지 파일과 각 이미지 설명, TTS 성별을 전달하면 쇼츠 생성 작업을 큐잉합니다.
            - images: 이미지 파일 배열 (최대 10장)
            - descriptions: 콤마(,) 구분 문자열. 파일 순서와 1:1 매칭
            - ttsGender: MALE | FEMALE
            응답은 즉시 QUEUED 상태의 uuid를 반환하며, 완료 여부는 /shorts/{uuid}로 폴링하세요.
            """
    )
    @PostMapping(
            value = "/stores/{storeId}/shorts",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResponse<ShortsDtoRes.ShortsCreateRes> create(
            @PathVariable Long storeId,
            @Parameter(
                    description = "이미지 파일 배열(최대 10장)",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))
                    )
            )
            @RequestPart("images") List<FilePart> images,
            @RequestPart("descriptions") String descriptions, // "설명1, 설명2, ..."
            @RequestPart("ttsGender") String ttsGender
    ) {
        List<String> descList = Arrays.stream(descriptions.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).toList();

        if (images == null || images.isEmpty()) {
            throw new GeneralException(ErrorStatus.SHORTS_IMAGE_EMPTY);
        }
        if (images.size() != descList.size()) {
            throw new GeneralException(ErrorStatus.SHORTS_IMAGE_COUNT_MISMATCH);
        }

        // 큐잉
        ShortsVideo sv = shortsCommandService.enqueue(storeId, images, descList, ttsGender);

        return ApiResponse.onSuccess(
                new ShortsDtoRes.ShortsCreateRes(sv.getUuid(), sv.getStatus().name())
        );
    }

    @Operation(
            summary = "쇼츠 상태/결과 조회",
            description = """
            쇼츠 생성 상태를 조회합니다.
            - status: QUEUED, PROCESSING, DONE, FAILED
            - url: DONE일 때 S3 URL(또는 null)
            """
    )
    @GetMapping(value = "/shorts/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ShortsDtoRes.ShortsDetailRes> get(@PathVariable String uuid) {
        ShortsDtoRes.ShortsDetailRes res = shortsQueryService.getByUuid(uuid);
        return ApiResponse.onSuccess(res);
    }

    @Operation(
            summary = "완료된 쇼츠 파일로 리다이렉트(302)",
            description = """
            DONE 상태의 쇼츠를 S3(또는 프리사인드 URL)로 302 리다이렉트합니다.
            - 아직 완료 전이면 SHORTS_NOT_READY 에러를 반환합니다.
            - 버킷/경로 변경 시에도 프론트는 이 엔드포인트만 사용하면 됩니다.
            """
    )
    @GetMapping("/shorts/{uuid}/file")
    public ResponseEntity<Void> redirectToS3(@PathVariable String uuid) {
        ShortsDtoRes.ShortsDetailRes res = shortsQueryService.getByUuid(uuid);
        if (res.url() == null) {
            throw new GeneralException(ErrorStatus.SHORTS_NOT_READY);
        }
        HttpHeaders h = new HttpHeaders();
        h.setLocation(URI.create(res.url()));
        return new ResponseEntity<>(h, HttpStatus.FOUND);
    }

    @Operation(
            summary = "쇼츠 생성 요청(저장된 이미지/JSON)",
            description = """
    이미 S3에 저장된 가게 이미지(uuid)와 각 이미지 설명, TTS 성별을 JSON으로 보내면
    서버가 이미지를 임시 파일로 다운로드한 뒤 파이썬 파이프라인에 전달합니다.
    - imageUuids: 업로드 API에서 받은 StoreImage.uuid 목록
    - descriptions: imageUuids와 1:1 매칭
    - ttsGender: MALE | FEMALE
    응답은 즉시 QUEUED 상태의 uuid를 반환하며, 완료 여부는 /api/v1/shorts/{uuid}로 폴링하세요.
    """
    )
    @PostMapping(
            value = "/stores/{storeId}/shorts:from-stored",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResponse<ShortsDtoRes.ShortsCreateRes> createFromStored(
            @PathVariable Long storeId,
            @RequestBody @Valid ShortsDtoReq.FromStoredImages req
    ) {
        ShortsVideo sv = shortsCommandService.enqueueFromStored(
                storeId, req.imageUuids(), req.descriptions(), req.ttsGender());

        return ApiResponse.onSuccess(
                new ShortsDtoRes.ShortsCreateRes(sv.getUuid(), sv.getStatus().name())
        );
    }
    @Operation(
            summary = "쇼츠 생성 요청(저장된 이미지 ID/JSON)",
            description = """
    DB에 저장된 가게 이미지 id만 보내면 서버가 해당 이미지의 uuid/url/type/description을 조회해
    파이썬 파이프라인으로 전달합니다.
    - imageIds: StoreImage.id 목록(요청 순서가 영상 순서)
    - ttsGender: MALE | FEMALE
    응답은 즉시 QUEUED 상태의 uuid를 반환합니다. 완료 여부는 GET /api/v1/shorts/{uuid}.
    """
    )
    @PostMapping(
            value = "/stores/{storeId}/shorts:from-stored-ids",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResponse<ShortsDtoRes.ShortsCreateRes> createFromStoredIds(
            @PathVariable Long storeId,
            @RequestBody @Valid ShortsDtoReq.FromStoredIds req
    ) {
        ShortsVideo sv = shortsCommandService.enqueueFromStoredIds(
                storeId, req.imageIds(), req.ttsGender());
        return ApiResponse.onSuccess(new ShortsDtoRes.ShortsCreateRes(sv.getUuid(), sv.getStatus().name()));
    }


}
