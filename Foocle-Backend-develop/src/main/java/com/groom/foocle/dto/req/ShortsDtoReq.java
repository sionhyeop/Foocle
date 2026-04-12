package com.groom.foocle.dto.req;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ShortsDtoReq {
    public record FromStoredImages(
            @NotEmpty
            List<String> imageUuids,

            @NotEmpty
            List<String> descriptions,

            String ttsGender
    ) {}

    public record FromStoredIds(
            @NotEmpty List<Long> imageIds,
            @NotEmpty String ttsGender
    ) {}
}
