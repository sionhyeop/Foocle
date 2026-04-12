package com.groom.foocle.service;

import com.groom.foocle.apiPayload.code.exception.GeneralException;
import com.groom.foocle.apiPayload.code.status.ErrorStatus;
import com.groom.foocle.common.client.PythonAiReactiveClient;
import com.groom.foocle.global.util.AmazonS3Util;
import com.groom.foocle.entity.ShortsVideo;
import com.groom.foocle.repository.ShortsVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.*;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortsGenerateWorker {

    private final ShortsVideoRepository shortsVideoRepository;
    private final PythonAiReactiveClient pythonClient;
    private final AmazonS3Util amazonS3Util;
    private final ShortsTxService shortsTxService;

    @Value("${app.temp.upload-dir:/tmp/foocle}")
    private String tempDir;

    @Async
    public void generateAsync(Long svId, String storeInfo, List<String> descList, String ttsGender, List<Path> tempImages) {
        ShortsVideo sv = shortsVideoRepository.findById(svId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SHORTS_NOT_FOUND));

        Path mp4 = Paths.get(tempDir, sv.getUuid(), "out.mp4");
        try {
            // PROCESSING
            shortsTxService.markProcessing(sv.getId());

            // 파이썬 (쇼츠 생성 api)호출
            ClientResponse resp = pythonClient.generateMergedVideoDynamic(storeInfo, descList, ttsGender, tempImages).block();
            if (resp == null || !resp.statusCode().is2xxSuccessful()) {
                if (resp != null) log.warn("Python error body: {}", resp.bodyToMono(String.class).block());
                throw new GeneralException(ErrorStatus.PYTHON_BAD_REQUEST);
            }

            //mp4 저장
            Files.createDirectories(mp4.getParent());
            try (AsynchronousFileChannel ch = AsynchronousFileChannel.open(
                    mp4, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                DataBufferUtils.write(resp.bodyToFlux(DataBuffer.class), ch, 0).blockLast();
            }

            // S3 업로드 → DONE
            String fileName = sv.getStore().getId() + "/" + sv.getUuid() + ".mp4";
            String url = amazonS3Util.uploadShortsVideoFromPath(mp4, fileName);
            // 홍보글 생성 시도, 실패해도 진행
            String promo = null;
            try {
                ClientResponse introResp = pythonClient.generateStoreIntro(storeInfo, descList).block();
                if (introResp != null && introResp.statusCode().is2xxSuccessful()) {
                    String body = introResp.bodyToMono(String.class).block();
                    promo = extractStoreIntro(body); // JSON에서 store_intro 추출
                } else {
                    log.warn("Store intro generation failed or null response.");
                }
            } catch (Exception e) {
                log.warn("Store intro generation error. continue without promo", e);
            }

            // 6) DONE + (있으면) 홍보글까지 저장
            shortsTxService.markDoneWithUrlAndPromo(sv.getId(), url, promo);

        } catch (Exception e) {
            log.error("Shorts generate failed. svId={}", svId, e);
            shortsTxService.markFailed(sv.getId());
        } finally {
            cleanTemp(tempImages, mp4);
        }
    }

    private void cleanTemp(List<Path> images, Path mp4) {
        images.forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
        try { Files.deleteIfExists(mp4); } catch (Exception ignored) {}
        Path parent = mp4.getParent();
        try { if (parent != null) Files.deleteIfExists(parent); } catch (Exception ignored) {}
    }

    /** 파이썬 응답 JSON에서 "store_intro"만 뽑기 */
    private String extractStoreIntro(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = om.readTree(json);
            com.fasterxml.jackson.databind.JsonNode n = root.get("store_intro");
            return (n != null && !n.isNull()) ? n.asText() : null;
        } catch (Exception e) {
            log.warn("Failed to parse store intro json: {}", json, e);
            return null;
        }
    }
}
