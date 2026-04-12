package com.groom.foocle.service;

import com.groom.foocle.apiPayload.code.exception.GeneralException;
import com.groom.foocle.apiPayload.code.status.ErrorStatus;
import com.groom.foocle.entity.ShortsVideo;
import com.groom.foocle.entity.Store;
import com.groom.foocle.entity.StoreImage;
import com.groom.foocle.entity.enums.ShortsStatus;
import com.groom.foocle.repository.ShortsVideoRepository;
import com.groom.foocle.repository.StoreImageRepository;
import com.groom.foocle.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShortsCommandService {

    private final StoreRepository storeRepository;
    private final StoreImageRepository storeImageRepository;
    private final ShortsVideoRepository shortsVideoRepository;
    private final ShortsGenerateWorker worker; //  분리된 비동기 작업자

    @Value("${app.temp.upload-dir:/tmp/foocle}") private String tempDir;

    @Transactional
    public ShortsVideo enqueue(Long storeId, List<FilePart> files, List<String> descList, String ttsGender) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.STORE_NOT_FOUND));

        ShortsVideo sv = ShortsVideo.queued(store, "Auto Shorts - " + System.currentTimeMillis());
        shortsVideoRepository.save(sv);

        List<Path> tempImages = saveToTempFiles(sv.getUuid(), files);
        worker.generateAsync(sv.getId(), storeInfo(store), descList, ttsGender, tempImages);

        return sv;
    }

    private String storeInfo(Store s) {
        String categories = s.getStoreCategoryMaps().stream()
                .map(m -> m.getStoreCategory().getCategory())
                .collect(Collectors.joining(", "));

        String openingHours = (s.getOpenTime() != null && s.getCloseTime() != null)
                ? s.getOpenTime() + " ~ " + s.getCloseTime()
                : "정보 없음";

        return """
            상호: %s
            주소: %s
            운영시간: %s
            카테고리: %s
            """.formatted(s.getName(), s.getAddress(), openingHours, categories);
    }

    private List<Path> saveToTempFiles(String uuid, List<FilePart> files) {
        Path base = Paths.get(tempDir, uuid);
        try { Files.createDirectories(base); } catch (IOException ignored) {}
        return files.stream().map(fp -> {
            Path out = base.resolve(UUID.randomUUID() + "_" + fp.filename());
            try (AsynchronousFileChannel ch = AsynchronousFileChannel.open(out,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                DataBufferUtils.write(fp.content(), ch, 0).blockLast();
                return out;
            } catch (IOException e) {
                throw new GeneralException(ErrorStatus.FILE_SAVE_FAILED);
            }
        }).toList();
    }


    @Transactional
    public ShortsVideo enqueueFromStored(Long storeId,
                                         List<String> imageUuids,
                                         List<String> descList,
                                         String ttsGender) {
        if (imageUuids == null || imageUuids.isEmpty()) {
            throw new GeneralException(ErrorStatus.SHORTS_IMAGE_EMPTY);
        }
        if (descList == null || imageUuids.size() != descList.size()) {
            throw new GeneralException(ErrorStatus.SHORTS_IMAGE_COUNT_MISMATCH);
        }

        var store = storeRepository.findById(storeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.STORE_NOT_FOUND));

        // 소유 가게의 이미지인지 검증하며 조회
        List<StoreImage> images = storeImageRepository.findAllByStore_IdAndUuidIn(storeId, imageUuids);
        if (images.size() != imageUuids.size()) {
            // 존재하지 않거나, 다른 가게 이미지가 섞인 경우
            throw new GeneralException(ErrorStatus.SHORTS_IMAGE_NOT_FOUND);
        }

        String uuid = java.util.UUID.randomUUID().toString();

        // 엔티티 생성(QUEUED)
        ShortsVideo sv = ShortsVideo.builder()
                .uuid(uuid)
                .store(store)
                .title("Auto Shorts - " + System.currentTimeMillis())
                .status(ShortsStatus.QUEUED)
                .build();
        shortsVideoRepository.save(sv);

        // 이미지(URL) → 임시 파일로 다운로드
        List<Path> tempImages = downloadToTempFiles(sv.getUuid().toString(), images);

        // 비동기 생성 트리거
        worker.generateAsync(sv.getId(), storeInfo(store), descList, ttsGender, tempImages);

        return sv;
    }

    @Transactional
    public ShortsVideo enqueueFromStoredIds(Long storeId, List<Long> imageIds, String ttsGender) {
        if (imageIds == null || imageIds.isEmpty()) {
            throw new GeneralException(ErrorStatus.SHORTS_IMAGE_EMPTY);
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.STORE_NOT_FOUND));

        // 소유 가게 검증 포함 조회
        List<StoreImage> images = storeImageRepository.findAllByStore_IdAndIdIn(storeId, imageIds);
        if (images.size() != imageIds.size()) {
            throw new GeneralException(ErrorStatus.SHORTS_IMAGE_NOT_FOUND);
        }

        // 요청 순서 보장
        Map<Long, StoreImage> map = images.stream().collect(Collectors.toMap(StoreImage::getId, i -> i));
        List<StoreImage> ordered = imageIds.stream().map(id -> {
            StoreImage img = map.get(id);
            if (img == null) throw new GeneralException(ErrorStatus.SHORTS_IMAGE_NOT_FOUND);
            return img;
        }).toList();

        // [타입] 설명 형태로 합치기 (예: "[FOOD] 김치참치김밥")
        List<String> descList = ordered.stream()
                .map(img -> "[" + img.getType().name() + "] " + (img.getDescription() == null ? "" : img.getDescription()))
                .toList();

        String uuid = java.util.UUID.randomUUID().toString();

        // 엔티티 생성(QUEUED)
        ShortsVideo sv = ShortsVideo.builder()
                .uuid(uuid)
                .store(store)
                .title("Auto Shorts - " + System.currentTimeMillis())
                .status(ShortsStatus.QUEUED)
                .build();
        shortsVideoRepository.save(sv);

        // 이미지 URL → 임시 파일
        List<Path> tempImages = downloadToTempFiles(sv.getUuid(), ordered);

        // 비동기 생성 트리거
        worker.generateAsync(sv.getId(), storeInfo(store), descList, ttsGender, tempImages);

        return sv;
    }

    private List<Path> downloadToTempFiles(String uuid, List<StoreImage> images) {
        Path base = Paths.get(tempDir, uuid);
        try { Files.createDirectories(base); } catch (Exception ignored) {}

        return images.stream().map(img -> {
            String ext = guessExt(img.getUrl()); // 간단 확장자 추정
            Path out = base.resolve(UUID.randomUUID() + ext);
            try (InputStream in = new URL(img.getUrl()).openStream()) {
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                return out;
            } catch (Exception e) {
                throw new GeneralException(ErrorStatus.FILE_DOWNLOAD_FAILED);
            }
        }).toList();
    }

    private String guessExt(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png")) return ".png";
        if (lower.contains(".jpeg")) return ".jpeg";
        if (lower.contains(".jpg")) return ".jpg";
        return ".jpg";
    }
}
