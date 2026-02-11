package com.karaoke_management.controller;

import com.karaoke_management.entity.Product;
import com.karaoke_management.entity.ProductCategory;
import com.karaoke_management.service.ProductCategoryService;
import com.karaoke_management.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final ProductCategoryService categoryService;

    public ProductController(ProductService productService, ProductCategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("product", new Product());
        return "product/product-list";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("products", productService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("product", productService.getById(id));
        model.addAttribute("editing", true);
        return "product/product-list";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("product") Product product,
                       @RequestParam(value = "categoryId", required = false) Long categoryId,
                       RedirectAttributes ra) {
        if (categoryId != null) {
            ProductCategory c = new ProductCategory();
            c.setId(categoryId);
            product.setCategory(c);
        } else {
            product.setCategory(null);
        }
        productService.save(product);
        ra.addFlashAttribute("msg", "Đã lưu sản phẩm");
        return "redirect:/products";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        productService.toggleActive(id);
        ra.addFlashAttribute("msg", "Đã đổi trạng thái");
        return "redirect:/products";
    }
}
