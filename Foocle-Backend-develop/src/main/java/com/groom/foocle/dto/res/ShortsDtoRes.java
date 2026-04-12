package com.groom.foocle.dto.res;

public class ShortsDtoRes {
    public record ShortsCreateRes(String shortsUuid, String status) {}

    public record ShortsDetailRes(
            String uuid, String status, String url, String title, String createdAt,String promotionText
    ) {}
}
