package com.groom.foocle.entity;

import com.groom.foocle.entity.enums.ShortsStatus;
import com.groom.foocle.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ShortsVideo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uuid;

    private String title; //추천 제목

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String promotionText;

    private String tag; //추천 태그

    private String fileName;

    private String url;

    // 파이프라인 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShortsStatus status; // QUEUED, PROCESSING, DONE, FAILED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @Setter(AccessLevel.PACKAGE)
    private Store store;

    // QUEUED 상태로 생성할 때 사용
    public static ShortsVideo queued(Store store, String title) {
        return ShortsVideo.builder()
                .uuid(UUID.randomUUID().toString())   // 생성 시점에 uuid 부여
                .store(store)
                .title(title)
                .status(ShortsStatus.QUEUED)
                .build();
    }

    //Shorts 생성 상태 전이 메서드
    public void markQueued() { this.status = ShortsStatus.QUEUED; }
    public void markProcessing() { this.status = ShortsStatus.PROCESSING; }
    public void markDoneWithUrl(String url) {
        this.status = ShortsStatus.DONE;
        this.url = url;
        // 파일명 규칙 통일(예: {uuid}.mp4). 필요 시 storeId/uuid.mp4 유지 가능
        this.fileName = this.uuid + ".mp4";
    }
    public void markFailed() {this.status = ShortsStatus.FAILED;
    }

    public void updatePromotionText(String promotionText) {
        this.promotionText = (promotionText == null || promotionText.isBlank())
                ? null
                : promotionText.trim();
    }
}
