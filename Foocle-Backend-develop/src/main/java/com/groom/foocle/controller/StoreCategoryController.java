package com.groom.foocle.controller;

import com.groom.foocle.apiPayload.ApiResponse;
import com.groom.foocle.dto.res.CategoryDtoRes;
import com.groom.foocle.service.StoreCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store-categories")
public class StoreCategoryController {

    private final StoreCategoryService categoryService;

    @Operation(summary = "카테고리 목록", description = "가게 등록에 사용할 카테고리 마스터 목록을 조회합니다.")
    @GetMapping("")
    public ApiResponse<List<CategoryDtoRes.Item>> getCategories() {
        return ApiResponse.onSuccess(categoryService.list());
    }
}
