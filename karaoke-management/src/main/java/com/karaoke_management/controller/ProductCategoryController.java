package com.karaoke_management.controller;

import com.karaoke_management.entity.ProductCategory;
import com.karaoke_management.service.ProductCategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/product-categories")
public class ProductCategoryController {

    private final ProductCategoryService categoryService;

    public ProductCategoryController(ProductCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("category", new ProductCategory());
        return "product/category-list";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("category") ProductCategory category,
                       RedirectAttributes ra) {
        categoryService.save(category);
        ra.addFlashAttribute("msg", "Đã lưu danh mục");
        return "redirect:/product-categories";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("category", categoryService.getById(id));
        model.addAttribute("editing", true);
        return "product/category-list";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        categoryService.toggleActive(id);
        ra.addFlashAttribute("msg", "Đã đổi trạng thái");
        return "redirect:/product-categories";
    }
}
