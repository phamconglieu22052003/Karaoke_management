package com.karaoke_management.service;

import com.karaoke_management.entity.Product;

import java.util.List;

public interface ProductService {
    List<Product> findAll();
    List<Product> findActiveWithCategory();
    Product getById(Long id);
    Product save(Product product);
    void toggleActive(Long id);
}
