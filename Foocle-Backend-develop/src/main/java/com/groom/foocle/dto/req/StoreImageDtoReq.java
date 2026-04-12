package com.groom.foocle.dto.req;

import com.groom.foocle.entity.enums.StoreImageType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class StoreImageDtoReq {

    public record Upload(
            @NotNull(message = "이미지 타입(type)은 필수입니다.")
            StoreImageType type,
            List<String> descriptions
    ) {}
}
