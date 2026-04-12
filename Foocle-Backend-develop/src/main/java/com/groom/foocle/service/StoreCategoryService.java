package com.groom.foocle.service;

import com.groom.foocle.dto.res.CategoryDtoRes;
import com.groom.foocle.repository.StoreCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreCategoryService {
    private final StoreCategoryRepository repo;

    @Transactional(readOnly = true)
    public List<CategoryDtoRes.Item> list() {
        return repo.findAll().stream()
                .map(c -> new CategoryDtoRes.Item(c.getId(), c.getCategory()))
                .toList();
    }
}