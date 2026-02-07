package com.karaoke_management.controller;

import com.karaoke_management.entity.ProductCategory;
import com.karaoke_management.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/product-categories")
public class ProductCategoryController {

    private final ProductCategoryRepository repo;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", repo.findAll());
        return "product-categories/category-list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("category", new ProductCategory());
        return "product-categories/category-form";
    }

    @PostMapping
    public String create(@ModelAttribute ProductCategory category) {
        repo.save(category);
        return "redirect:/product-categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("category", repo.findById(id).orElseThrow());
        return "product-categories/category-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute ProductCategory category) {
        ProductCategory existing = repo.findById(id).orElseThrow();
        existing.setName(category.getName());
        existing.setActive(category.isActive());
        repo.save(existing);
        return "redirect:/product-categories";
    }
}
