package com.karaoke_management.service.impl;

import com.karaoke_management.entity.ProductCategory;
import com.karaoke_management.repository.ProductCategoryRepository;
import com.karaoke_management.service.ProductCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryRepository repository;

    public ProductCategoryServiceImpl(ProductCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProductCategory> findAll() {
        return repository.findAll();
    }

    @Override
    public ProductCategory getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));
    }

    @Override
    public ProductCategory save(ProductCategory category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên danh mục không được để trống");
        }
        category.setName(category.getName().trim());
        return repository.save(category);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        ProductCategory c = getById(id);
        c.setActive(c.getActive() == null ? Boolean.TRUE : !c.getActive());
        repository.save(c);
    }
}
