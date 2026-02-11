package com.karaoke_management.service;

import com.karaoke_management.entity.ProductCategory;

import java.util.List;

public interface ProductCategoryService {
    List<ProductCategory> findAll();
    ProductCategory getById(Long id);
    ProductCategory save(ProductCategory category);
    void toggleActive(Long id);
}
