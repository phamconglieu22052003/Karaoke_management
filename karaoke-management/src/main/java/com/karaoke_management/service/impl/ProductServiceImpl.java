package com.karaoke_management.service.impl;

import com.karaoke_management.entity.Product;
import com.karaoke_management.entity.ProductCategory;
import com.karaoke_management.repository.ProductCategoryRepository;
import com.karaoke_management.repository.ProductRepository;
import com.karaoke_management.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, ProductCategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> findActiveWithCategory() {
        return productRepository.findActiveWithCategory();
    }

    @Override
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
    }

    @Override
    @Transactional
    public Product save(Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên sản phẩm không được để trống");
        }
        product.setName(product.getName().trim());

        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá sản phẩm không hợp lệ");
        }

        // gắn lại category nếu có id
        ProductCategory cat = null;
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            cat = categoryRepository.findById(product.getCategory().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục không hợp lệ"));
        }
        product.setCategory(cat);

        if (product.getActive() == null) product.setActive(true);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        Product p = getById(id);
        p.setActive(p.getActive() == null ? Boolean.TRUE : !p.getActive());
        productRepository.save(p);
    }
}
