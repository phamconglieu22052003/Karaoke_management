package com.karaoke_management.controller;

import com.karaoke_management.entity.Product;
import com.karaoke_management.repository.ProductCategoryRepository;
import com.karaoke_management.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "products/product-list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepository.findAll());
        return "products/product-form";
    }

    @PostMapping
    public String create(@ModelAttribute Product product, @RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            product.setCategory(categoryRepository.findById(categoryId).orElse(null));
        }
        productRepository.save(product);
        return "redirect:/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id).orElseThrow();
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryRepository.findAll());
        return "products/product-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Product product, @RequestParam(required = false) Long categoryId) {
        Product existing = productRepository.findById(id).orElseThrow();
        existing.setName(product.getName());
        existing.setPrice(product.getPrice());
        existing.setUnit(product.getUnit());
        existing.setActive(product.isActive());

        if (categoryId != null) {
            existing.setCategory(categoryRepository.findById(categoryId).orElse(null));
        } else {
            existing.setCategory(null);
        }

        productRepository.save(existing);
        return "redirect:/products";
    }
}
