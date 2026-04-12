package com.groom.foocle.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public class StoreDtoReq {
    public record Create(
            @NotBlank(message = "이름은 필수입니다.")
            String name,
            @NotBlank(message = "주소는 필수입니다.")
            String address,
            @NotNull(message = "오픈시간은 필수입니다.")
            LocalTime openTime,
            @NotNull(message = "오프시간은 필수입니다.")
            LocalTime closeTime,
            @NotEmpty(message = "카테고리ID는 필수입니다.")
            List<Long> categoryIds
    ) {}
}
